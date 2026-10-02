# language: es
@WS-US-03
Característica: Inicio de sesión con contraseña
  Como usuario verificado de Clair
  Quiero iniciar sesión con mi correo y contraseña
  Para obtener un token de acceso a mis espacios y dispositivos

  Esquema del escenario: Autenticar credenciales
    Dado que existe un usuario verificado con correo "ana@clair.pe" y contraseña "Clair@2026"
    Cuando inicia sesión con correo "<correo>" y contraseña "<contrasena>"
    Entonces la respuesta tiene estado <estado>

    Ejemplos:
      | correo         | contrasena | estado |
      | ana@clair.pe   | Clair@2026 | 200    |
      | ana@clair.pe   | Incorrecta | 401    |
      | nadie@clair.pe | Clair@2026 | 401    |
