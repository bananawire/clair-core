# Historias de Usuario - Notificaciones (Notifications)

Esta sección presenta las Epics e Historias Técnicas identificadas para el módulo de Notificaciones (Notifications).

## Epics

| Epic / Story ID | Título | Descripción | Criterios de Aceptación | Relacionado con (Epic ID) |
| --------------- | ------ | ----------- | ----------------------- | ------------------------- |
| WS-EP-10         | Gestión y Logs de Notificaciones | Como Usuario, quiero visualizar el historial de notificaciones push enviadas a mi cuenta, para estar al tanto de los avisos y alertas del sistema. | La Epic se completa cuando todas las historias de usuario relacionadas han sido implementadas, validadas y aceptadas. | - |

## Historias Técnicas

| Epic / Story ID | Título | Descripción | Criterios de Aceptación | Relacionado con (Epic ID) |
| --------------- | ------ | ----------- | ----------------------- | ------------------------- |
| WS-US-52           | Obtener Historial de Notificaciones Push del Usuario | Como Desarrollador, quiero recuperar la lista paginada de registros de notificaciones push enviadas a un usuario a través de una API, para mostrar su historial de alertas en el panel de control. | Escenario: Obtener historial de notificaciones del usuario de forma exitosa<br>Dado que el endpoint "/api/v1/notifications/push" está disponible<br>Cuando se envía una petición GET por parte de un usuario autenticado especificando el parámetro `page`<br>Entonces se recibe una respuesta con estado 200 conteniendo los registros de notificaciones push del usuario, paginados con tamaño de 20 elementos y ordenados por fecha descendente. | WS-EP-10 |
