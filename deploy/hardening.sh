#!/usr/bin/env bash
# Fase 1: asegurar el servidor. Ejecutar como root en el VPS recien creado.
#
#   bash hardening.sh <usuario-nuevo> [hostname]
#
# Un VPS con IP publica empieza a recibir intentos de login automatizados a los pocos minutos
# de existir. Esto va ANTES de subir la aplicacion.
set -euo pipefail

USUARIO="${1:-pipoe}"
HOSTNAME_NUEVO="${2:-}"

. /etc/os-release
DISTRO="${ID}"
CODENAME="${VERSION_CODENAME:-${UBUNTU_CODENAME:-}}"

echo "==> Sistema: ${PRETTY_NAME}"

if [ -n "${HOSTNAME_NUEVO}" ]; then
    echo "==> Hostname: ${HOSTNAME_NUEVO}"
    hostnamectl set-hostname "${HOSTNAME_NUEVO}"
    if ! grep -q "127.0.1.1[[:space:]]*${HOSTNAME_NUEVO}" /etc/hosts; then
        sed -i "/^127\.0\.1\.1/d" /etc/hosts
        echo "127.0.1.1 ${HOSTNAME_NUEVO}" >> /etc/hosts
    fi
fi

echo "==> Actualizando el sistema"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get upgrade -y -qq

echo "==> Instalando utilidades"
apt-get install -y -qq ufw fail2ban unattended-upgrades ca-certificates curl git

echo "==> Zona horaria en UTC"
timedatectl set-timezone UTC

echo "==> Docker"
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    echo "    ya instalado: $(docker --version)"
else
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL "https://download.docker.com/linux/${DISTRO}/gpg" -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/${DISTRO} ${CODENAME} stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable --now docker
    echo "    instalado: $(docker --version)"
fi

echo "==> Creando el usuario ${USUARIO} con sudo"
if ! id -u "${USUARIO}" >/dev/null 2>&1; then
    adduser --disabled-password --gecos "" "${USUARIO}"
fi
usermod -aG sudo "${USUARIO}"
# Sin este grupo, el usuario no puede hablar con el demonio de Docker y el compose falla dos
# fases despues, con un error que no menciona los permisos. Nunca silenciarlo con `|| true`.
usermod -aG docker "${USUARIO}"

# La cuenta se crea con --disabled-password para que solo se entre con clave SSH. Sin esto,
# `sudo` pediria una contrasena que no existe y el usuario quedaria sin poder elevar
# privilegios: con el acceso de root ya cerrado, sin forma de administrar el servidor.
#
# Sudo sin contrasena y no una contrasena nueva, por dos motivos:
#   1. La credencial real es la clave SSH. Un segundo secreto, mas debil y que hay que
#      recordar, no protege nada: quien no tenga la clave no llega ni a la pantalla de login.
#   2. El usuario ya esta en el grupo docker, que es equivalente a root: con acceso al demonio
#      se puede montar el disco del host dentro de un contenedor. Pedir contrasena para sudo
#      mientras se otorga eso es un cartel de prohibido al lado de una puerta abierta.
# Es el mismo patron de las imagenes de nube: el usuario `ubuntu` de AWS viene asi.
echo "==> Sudo sin contrasena para ${USUARIO}"
echo "${USUARIO} ALL=(ALL) NOPASSWD:ALL" > "/etc/sudoers.d/90-${USUARIO}"
chmod 440 "/etc/sudoers.d/90-${USUARIO}"
# Se valida antes de seguir: un sudoers con un error de sintaxis deja al sistema entero sin
# forma de elevar privilegios, y esto corre justo antes de cerrar el acceso de root.
visudo -c

echo "==> Copiando la clave SSH de root al usuario nuevo"
if [ ! -s /root/.ssh/authorized_keys ]; then
    echo
    echo "ERROR: root no tiene ninguna clave SSH en /root/.ssh/authorized_keys."
    echo "Sin eso, cerrar el acceso por contrasena te dejaria afuera del servidor."
    exit 1
fi
mkdir -p "/home/${USUARIO}/.ssh"
cp /root/.ssh/authorized_keys "/home/${USUARIO}/.ssh/authorized_keys"
chown -R "${USUARIO}:${USUARIO}" "/home/${USUARIO}/.ssh"
chmod 700 "/home/${USUARIO}/.ssh"
chmod 600 "/home/${USUARIO}/.ssh/authorized_keys"

echo "==> Swap de 4 GB"
# La maquina contratada tiene 4 GB y un solo nucleo, y las imagenes se construyen ahi mismo.
# Sin swap, cuando la RAM se llena el kernel no frena nada: elige el proceso mas grande y lo
# mata, que aca es la JVM o Postgres. Con swap, un pico se resuelve con lentitud en vez de con
# un proceso muerto.
if swapon --show | grep -q '/swapfile'; then
    echo "    ya existe"
else
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
    swapon /swapfile
    grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo "    creado"
fi
# Que el kernel lo toque solo bajo presion real y no para cachear archivos.
echo 'vm.swappiness=10' > /etc/sysctl.d/99-swap.conf
sysctl --system >/dev/null

echo "==> Firewall: solo SSH, HTTP y HTTPS"
ufw --force reset >/dev/null
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'
ufw --force enable

echo "==> fail2ban para SSH"
cat > /etc/fail2ban/jail.local <<'JAIL'
[sshd]
enabled  = true
port     = ssh
backend  = systemd
maxretry = 5
findtime = 10m
bantime  = 1h
JAIL
systemctl enable --now fail2ban
systemctl restart fail2ban

echo "==> Actualizaciones de seguridad automaticas"
dpkg-reconfigure -f noninteractive unattended-upgrades

echo
free -h | head -2
echo
echo "Nota: ${USUARIO} usa sudo SIN contrasena. La cuenta no tiene ninguna: se entra por clave."
echo
echo "LISTO. Antes de cerrar esta sesion, abri OTRA terminal y verifica que entras:"
echo "    ssh ${USUARIO}@$(hostname -I | awk '{print $1}')"
echo "Recien cuando eso funcione, corre cerrar-acceso-root.sh"
