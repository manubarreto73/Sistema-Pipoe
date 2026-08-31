#!/usr/bin/env bash
# Fase 1 (segunda parte): deshabilitar contrasenas y login de root por SSH.
#
# Ejecutar SOLO despues de confirmar, en otra terminal, que entras con el usuario nuevo y su
# clave. Este paso es irreversible desde afuera: si la clave no quedo bien y se cierra la
# unica sesion abierta, la unica salida es la consola de recuperacion del proveedor.
set -euo pipefail

echo "==> Deshabilitando login por contrasena y acceso directo de root"
cat > /etc/ssh/sshd_config.d/99-endurecido.conf <<'SSHD'
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
ChallengeResponseAuthentication no
PubkeyAuthentication yes
X11Forwarding no
MaxAuthTries 3
SSHD

# Valida la sintaxis ANTES de recargar: una configuracion invalida dejaria a sshd sin arrancar.
sshd -t

# reload y no restart: no corta las sesiones abiertas.
systemctl reload ssh 2>/dev/null || systemctl reload sshd

echo
echo "LISTO. La sesion actual sigue abierta."
echo "Verifica en OTRA terminal que seguis entrando ANTES de cerrar esta."
