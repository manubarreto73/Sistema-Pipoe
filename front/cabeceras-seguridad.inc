# Cabeceras de seguridad de la página. Van en un archivo aparte porque nginx no las hereda:
# si un `location` define su propio add_header, pierde todos los del bloque padre. Repetir el
# include es la forma de que no se caiga ninguno por olvido.

# Content-Security-Policy: qué tiene permitido cargar y ejecutar la página. Es la defensa de
# fondo contra XSS — aunque alguien logre inyectar una etiqueta <script>, el navegador se
# niega a ejecutarla si no viene del propio sitio.
#   'unsafe-inline' en style-src es necesario: el editor de texto aplica estilos en el
#   atributo style de los elementos. En script-src NO está, que es donde importa.
#   connect-src 'self' alcanza porque la API se sirve bajo el mismo origen, en /api.
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'" always;

# Que el navegador no adivine el tipo de un archivo por su contenido.
add_header X-Content-Type-Options "nosniff" always;

# La página no se puede embeber en un iframe ajeno: evita el clickjacking.
add_header X-Frame-Options "DENY" always;

# Al salir del sitio no se filtra la ruta interna en la que estaba la persona.
add_header Referrer-Policy "strict-origin-when-cross-origin" always;

add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=()" always;

# HSTS: obliga a que las próximas visitas usen HTTPS. Sólo tiene efecto sobre HTTPS, así que
# en la prueba local no molesta.
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
