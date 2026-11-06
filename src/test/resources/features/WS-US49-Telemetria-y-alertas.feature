# language: es
@WS-US-49 @WS-US-34
Característica: Telemetría y alertas por umbral
  Como usuario dueño de un sensor Clair
  Quiero que cada lectura de telemetría se evalúe contra mis umbrales
  Para recibir una alerta solo cuando la calidad del aire empeora

  Escenario: Una lectura que supera el umbral genera una alerta
    Dado que el usuario tiene un dispositivo Clair reclamado con umbral de "PM25" en "50.00"
    Cuando el dispositivo envía las siguientes lecturas:
      | pm25 | co2   | temperatura | humedad |
      | 12.0 | 450.0 | 23.5        | 52.0    |
      | 18.5 | 470.0 | 23.8        | 51.0    |
      | 80.0 | 480.0 | 24.1        | 50.5    |
    Entonces se registran 3 lecturas de telemetría para el dispositivo
    Y el usuario tiene 1 alerta activa de "PM25" con severidad "CRITICAL"

  Escenario: Lecturas dentro del umbral no generan alertas
    Dado que el usuario tiene un dispositivo Clair reclamado con umbral de "CO2" en "1000.00"
    Cuando el dispositivo envía las siguientes lecturas:
      | pm25 | co2   | temperatura | humedad |
      | 10.0 | 600.0 | 22.0        | 55.0    |
      | 11.0 | 750.0 | 22.4        | 54.0    |
    Entonces se registran 2 lecturas de telemetría para el dispositivo
    Y el usuario no tiene alertas registradas
