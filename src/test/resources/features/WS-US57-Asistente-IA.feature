# language: es
@WS-US-57 @WS-US-58 @WS-US-59 @WS-US-60
Característica: Asistente IA de calidad del aire
  Como usuario de Clair
  Quiero preguntarle a un asistente por el aire de mis espacios
  Para entender mis lecturas y saber qué hacer sin interpretar los datos yo mismo

  Escenario: El asistente responde y descuenta un mensaje del cupo diario
    Dado que un usuario con plan "FREEMIUM" ha iniciado sesión
    Y el asistente responderá "Tu sala está bien ventilada."
    Cuando le pregunta al asistente "¿Cómo está el aire de mi sala?"
    Entonces la respuesta tiene estado 200
    Y la respuesta del asistente es "Tu sala está bien ventilada."
    Y al usuario le quedan 9 de 10 mensajes del asistente hoy

  Esquema del escenario: El cupo diario depende del plan
    Dado que un usuario con plan "<plan>" ha iniciado sesión
    Y ya envió <limite> mensajes al asistente hoy
    Cuando le pregunta al asistente "Hola"
    Entonces la respuesta tiene estado 429
    Y el asistente no fue consultado

    Ejemplos:
      | plan     | limite |
      | FREEMIUM | 10     |
      | PREMIUM  | 100    |

  Escenario: Si el modelo falla no se guarda nada ni se consume cupo
    Dado que un usuario con plan "FREEMIUM" ha iniciado sesión
    Y el asistente no está disponible
    Cuando le pregunta al asistente "¿Debo ventilar?"
    Entonces la respuesta tiene estado 503
    Y al usuario le quedan 10 de 10 mensajes del asistente hoy
    Y el usuario no tiene conversaciones con el asistente

  Escenario: El asistente recibe los datos del espacio pero nunca el correo del usuario
    Dado que el usuario tiene un dispositivo Clair reclamado en su espacio
    Y el asistente responderá "Revisa la ventilación."
    Cuando le pregunta al asistente "¿Qué pasa en mi espacio?"
    Entonces la respuesta tiene estado 200
    Y el contexto enviado al asistente menciona el espacio "Sala"
    Y el contexto enviado al asistente no incluye el correo del usuario

  Escenario: El usuario califica una respuesta y la conversación la conserva
    Dado que un usuario con plan "PREMIUM" ha iniciado sesión
    Y el asistente responderá "Abre una ventana diez minutos."
    Cuando le pregunta al asistente "¿Qué hago si el CO2 sube?"
    Y califica la respuesta del asistente con "UP"
    Entonces la respuesta tiene estado 204
    Y la conversación muestra la respuesta del asistente calificada con "UP"
