# language: es
@WS-US-18 @WS-US-19
Característica: Umbrales de métricas por dispositivo
  Como usuario dueño de un sensor Clair
  Quiero definir y ajustar el umbral de cada métrica de calidad del aire
  Para que el sistema me alerte cuando una lectura lo supere

  Esquema del escenario: Crear y actualizar el umbral de una métrica
    Dado que el usuario tiene un dispositivo Clair reclamado en su espacio
    Cuando crea un umbral para la métrica "<metrica>" con valor "<valor>"
    Entonces la respuesta tiene estado 201
    Y el dispositivo tiene un umbral activo de "<metrica>" con valor "<valor>"
    Cuando actualiza el umbral de la métrica "<metrica>" al valor "<nuevo_valor>"
    Entonces la respuesta tiene estado 200
    Y el dispositivo tiene un umbral activo de "<metrica>" con valor "<nuevo_valor>"

    Ejemplos:
      | metrica     | valor   | nuevo_valor |
      | PM25        | 35.00   | 50.00       |
      | CO2         | 1000.00 | 1200.00     |
      | TEMPERATURE | 28.50   | 30.00       |
      | HUMIDITY    | 70.00   | 75.50       |
