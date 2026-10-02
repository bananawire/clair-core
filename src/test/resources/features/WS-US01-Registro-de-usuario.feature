# language: es
@WS-US-01 @WS-US-02
Característica: Registro de usuario con código de verificación
  Como visitante de Clair
  Quiero registrarme con mi correo y una contraseña y confirmarlo con un código
  Para acceder a la plataforma como usuario verificado

  Escenario: Confirmar el registro con el código recibido por correo
    Dado que un visitante inicia su registro con un correo nuevo y la contraseña "Clair@2026"
    Cuando confirma el registro con el código de verificación recibido
    Entonces la respuesta tiene estado 201
    Y la cuenta queda activa y puede iniciar sesión con la contraseña "Clair@2026"

  Escenario: Rechazar la confirmación con un código inválido
    Dado que un visitante inicia su registro con un correo nuevo y la contraseña "Clair@2026"
    Cuando confirma el registro con el código "ZZZZ-ZZZZ"
    Entonces la respuesta tiene estado 400
    Y la cuenta no queda creada
