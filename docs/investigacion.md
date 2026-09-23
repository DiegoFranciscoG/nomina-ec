# Investigación y fuentes — nomina-ec

Fecha de consulta de todas las fuentes: **2026-09-23**. Regla de desempate: si dos fuentes se contradicen, gana la oficial y más reciente.
Cada regla del motor de cálculo apunta a una fila de esta tabla (columna `#`), y cada parámetro sembrado en `legal_parameters` guarda su `source_url`.

## 1. Fuentes

| # | Fuente | Tipo | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|---|
| F1 | Ministerio del Trabajo — boletín SBU 2026 | Oficial | https://www.trabajo.gob.ec/despues-de-casi-una-decada-hay-consenso-gobierno-empleadores-y-trabajadores-acuerdan-fijar-el-salario-basico-unificado-de-2026-en-usd-482-no-hay-imposicion-hay-union/ | 2026-09-23 | SBU 2026 = **USD 482,00** |
| F2 | Acuerdo Ministerial MDT-2025-195 (R.O. S. 187, 18-dic-2025), resumen de CorralRosales | Secundaria (estudio jurídico) | https://corralrosales.com/acuerdo-ministerial-mdt-2025-195-nuevo-salario-basico-unificado-2026/ | 2026-09-23 | Número del acuerdo, fecha de publicación y vigencia desde 2026-01-01 |
| F3 | Código del Trabajo, codificación 2005-017, última reforma R.O. 309-S 12-V-2023 (PDF publicado por el Ministerio del Trabajo) | Oficial | https://www.trabajo.gob.ec/wp-content/uploads/2024/01/CODIGO_DEL_TRABAJO-CT.pdf | 2026-09-23 | Arts. 47, 55, 69–71, 95, 97, 111–114, 185, 188, 196–197 (ver sección 2) |
| F4 | IESS — Fondos de reserva | Oficial | https://www.iess.gob.ec/en/web/afiliado/fondos-de-reserva | 2026-09-23 | 8,33 % desde el mes 13; el afiliado elige mensualizar o acumular en el IESS |
| F5 | Tablas de aportes IESS 2026 (misalario.ec, tagline-soluciones.com, rolesdepago.com) | Secundaria (3 fuentes coincidentes) | https://misalario.ec/tabla-aportes-iess-ecuador/ | 2026-09-23 | Aporte personal 9,45 %, patronal 11,15 %, IECE 0,5 %, SECAP 0,5 % (sector privado) |
| F6 | SRI — Resolución NAC-DGERCGC25-00000043 (2.º S. R.O. 194, 30-dic-2025) | Oficial | https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=bb7aac3c-251d-4243-9477-10a3ba8e7355&nombre=NAC-DGERCGC25-00000043.pdf | 2026-09-23 | Tabla de impuesto a la renta 2026 de personas naturales (sección 3) |
| F7 | NMS Law — transcripción de la tabla IR 2026 | Secundaria | https://nmslaw.com.ec/blog/2026/01/05/tablas-impuesto-renta-2026-ecuador/ | 2026-09-23 | Filas de la tabla (contrastadas con F6: fracción básica 12 208) |
| F8 | SRI — Boletín 006 "Proyección de gastos personales 2026" | Oficial | https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/ea002a89-17d8-4cdf-be42-affedbe3f8d8/BOLET%C3%8DN%20006%20-%20SRI%20HABILITA%20LA%20PROYECCI%C3%93N%20DE%20GASTOS%20PERSONALES%202026%20PARA%20REDUCIR%20EL%20IMPUESTO%20A%20LA%20RENTA.pdf | 2026-09-23 | Rebaja = 18 % del menor entre gastos proyectados y el límite en canastas |
| F9 | Primicias / Factuplan — tabla de cargas familiares 2026 | Secundaria (2 fuentes coincidentes) | https://factuplan.com.ec/blog/gastos-personales-deducibles-ecuador-2026 | 2026-09-23 | 0→7, 1→9, 2→11, 3→14, 4→17, 5+→20 canastas; enfermedad catastrófica → 100 canastas |
| F10 | Ley Orgánica de Eficiencia Económica y Generación de Empleo (R.O. 461-S, 20-dic-2023) | Oficial | https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/bc4b9983-05f1-4a7a-a7c6-8a7cb2ec5572/LEY%20ORG%C3%81NICA%20DE%20EFICIENCIA%20ECON%C3%93MICA%20Y%20GENERACI%C3%93N%20DE%20EMPLEO.pdf | 2026-09-23 | Base legal del límite de 100 canastas por discapacidad o enfermedad catastrófica |
| F11 | INEC — Canastas analíticas, enero 2026 | Oficial | https://www.ecuadorencifras.gob.ec/documentos/web-inec/Inflacion/canastas/2026/enero/1.Informe_Ejecutivo_Canastas_Analiticas_ene_2026.pdf | 2026-09-23 | Canasta familiar básica enero 2026 = **USD 821,80** |
| F12 | Ley de Régimen Tributario Interno (codificación) | Oficial | https://www.tce.gob.ec/wp-content/uploads/2022/01/Ley-de-Regimen-Tributario-Interno.pdf | 2026-09-23 | Art. 9: décimos y fondos de reserva exentos de IR; Art. 17: aporte personal IESS deducible |
| F13 | Reglamento de aplicación de la LRTI | Oficial | https://www.gob.ec/sites/default/files/regulations/2022-02/Reglamento-a-la-Ley-de-R%C3%A9gimen-Tributario-Interno.pdf | 2026-09-23 | Art. 104: retención mensual en relación de dependencia sobre la proyección anual |

## 2. Reglas laborales (Código del Trabajo, F3)

| Regla | Artículo | Cómo se implementa |
|---|---|---|
| Jornada máxima 8 h/día, 40 h/semana | Art. 47 | `contracts.weekly_hours` ≤ 40; valor hora = sueldo / (240 × horas semanales / 40) |
| Horas suplementarias (hasta 24:00): recargo **50 %**; máximo 4 h/día y 12 h/semana | Art. 55 num. 1–2 | Parámetro `OVERTIME_SUPPLEMENTARY_SURCHARGE = 0.50`; se valida máx. 12 × 4,33 ≈ 52 h/mes (tope `OVERTIME_SUPPLEMENTARY_MAX_MONTH = 48`, SUPUESTO conservador: 12 h × 4 semanas) |
| Horas extraordinarias (24:00–06:00, sábado, domingo): recargo **100 %** | Art. 55 num. 2 y 4 | Parámetro `OVERTIME_EXTRAORDINARY_SURCHARGE = 1.00` |
| Base de cálculo de la hora: 240 horas mensuales (30 días × 8 h) | Art. 55 + práctica MDT | Parámetro `MONTHLY_HOURS_BASE = 240` |
| Vacaciones: 15 días/año; +1 día por año desde el 6.º año, máximo 15 adicionales | Art. 69 | Parámetros `VACATION_BASE_DAYS = 15`, `VACATION_EXTRA_AFTER_YEARS = 5`, `VACATION_EXTRA_MAX_DAYS = 15` |
| Valor de vacaciones: 1/24 de lo percibido en el año | Art. 71 | Provisión mensual = base remunerativa / 24 |
| Remuneración para beneficios: incluye horas extra y comisiones; excluye utilidades, fondos de reserva, décimos, viáticos | Art. 95 | La "base remunerativa" suma solo conceptos marcados `iess_taxable` |
| Décimo tercero: 1/12 de lo percibido; se paga **mensualmente**; acumulado solo a pedido escrito, hasta el 24 de diciembre (ciclo 1-dic a 30-nov) | Art. 111 | `contracts.thirteenth_mode` = `MONTHLY` \| `ACCUMULATED` |
| Décimo tercero no es base de aportes IESS, fondos de reserva, vacaciones ni IR | Art. 112 | Concepto `THIRTEENTH_MONTHLY` con `iess_taxable = false`, `income_tax_taxable = false` |
| Décimo cuarto: 1/12 del SBU mensual; acumulado a pedido escrito hasta el 15-mar (Costa/Insular) o 15-ago (Sierra/Amazonía) | Art. 113 | `contracts.region` y `contracts.fourteenth_mode`; ciclos 1-mar→28/29-feb y 1-ago→31-jul |
| Salida antes de la fecha de pago: décimo cuarto proporcional | Art. 113 inc. final | Liquidación calcula días del ciclo × SBU / 360 |
| Utilidades: 15 % (10 % partes iguales, 5 % por cargas) | Art. 97 | Parámetros `PROFIT_SHARING_WORKERS = 0.10`, `PROFIT_SHARING_DEPENDENTS = 0.05` (registrados; cálculo anual fuera del MVP) |
| Bonificación por desahucio: 25 % de la última remuneración por cada año de servicio; también en terminación por mutuo acuerdo | Art. 185 | Parámetro `SEVERANCE_BONUS_RATE = 0.25` |
| Despido intempestivo: ≤ 3 años → 3 remuneraciones; > 3 años → 1 por año, máximo 25; la fracción de año cuenta como año completo | Art. 188 | Parámetros `DISMISSAL_MIN_MONTHS = 3`, `DISMISSAL_THRESHOLD_YEARS = 3`, `DISMISSAL_MAX_MONTHS = 25` |
| Fondo de reserva: un mes de sueldo por cada año posterior al primero (8,33 % mensual) | Art. 196–197 + F4 | Parámetro `RESERVE_FUND_RATE = 0.0833`; derecho desde el día 366; `contracts.reserve_fund_mode` = `MONTHLY` \| `ACCUMULATED` (IESS) |

## 3. Tabla de impuesto a la renta 2026 — personas naturales (F6, F7)

| Fracción básica | Exceso hasta | Impuesto fracción básica | % fracción excedente |
|---:|---:|---:|---:|
| 0 | 12 208 | 0 | 0 % |
| 12 208 | 15 549 | 0 | 5 % |
| 15 549 | 20 188 | 167 | 10 % |
| 20 188 | 26 700 | 631 | 12 % |
| 26 700 | 35 136 | 1 412 | 15 % |
| 35 136 | 46 575 | 2 678 | 20 % |
| 46 575 | 62 005 | 4 965 | 25 % |
| 62 005 | 82 679 | 8 823 | 30 % |
| 82 679 | 109 956 | 15 025 | 35 % |
| 109 956 | en adelante | 24 572 | 37 % |

## 4. Gastos personales 2026 (F8–F11)

- Rebaja del IR = **18 %** × mín(gastos personales proyectados, canastas × USD 821,80).
- Canastas según cargas familiares: 0 → 7 · 1 → 9 · 2 → 11 · 3 → 14 · 4 → 17 · 5 o más → 20.
- Titular o carga con discapacidad o enfermedad catastrófica, rara u huérfana: 100 canastas.
- Carga familiar: padres, cónyuge o conviviente, hijos hasta 21 años o con discapacidad; no pueden tener ingresos gravados mayores a un SBU.

## 5. Seguridad social (F4, F5)

| Concepto | Tasa | Quién paga | Parámetro |
|---|---:|---|---|
| Aporte personal IESS | 9,45 % | Trabajador (descuento) | `IESS_PERSONAL_RATE` |
| Aporte patronal IESS | 11,15 % | Empleador | `IESS_EMPLOYER_RATE` |
| IECE | 0,50 % | Empleador | `IECE_RATE` |
| SECAP | 0,50 % | Empleador | `SECAP_RATE` |
| Fondo de reserva | 8,33 % | Empleador, desde el mes 13 | `RESERVE_FUND_RATE` |

## 6. Método de retención mensual del IR (F12, F13)

1. Ingreso gravado del mes = base remunerativa (sueldo proporcional + horas extra + otros ingresos gravados) − aporte personal IESS. Décimos y fondos de reserva no se suman (LRTI Art. 9).
2. Proyección anual = gravado acumulado de meses anteriores del mismo año + gravado del mes × meses restantes (incluido el actual).
3. Impuesto anual = tabla(proyección) − rebaja por gastos personales (nunca negativo).
4. Retención del mes = (impuesto anual − retenido en meses anteriores) ÷ meses restantes, redondeado a 2 decimales con HALF_UP.

Este esquema reliquida automáticamente cuando cambia el sueldo o la proyección de gastos (Reglamento, Art. 104).

## 7. Supuestos (no verificados en fuente oficial primaria)

> Estos puntos deben revisarse si el sistema se usa con datos reales.

1. **Tasas IESS 9,45 % / 11,15 % / IECE 0,5 % / SECAP 0,5 %**: coinciden en tres fuentes secundarias y en el dato del kit (Ministerio del Trabajo / IESS vía vistazo.com y eluniverso.com), pero no se descargó la resolución del Consejo Directivo del IESS. Se guardan como parámetros editables.
2. **Tabla de cargas familiares (7/9/11/14/17/20 canastas)**: tomada de dos fuentes secundarias coincidentes; el boletín oficial del SRI (F8) confirma el rango 7–20 pero no se pudo extraer la tabla completa del PDF.
3. **Bonificación por desahucio con fracción de año**: el Art. 185 dice "por cada uno de los años"; el sistema la calcula proporcional a los días (días / 360), como hace la calculadora del Ministerio del Trabajo. Parámetro `SEVERANCE_PRORATE_FRACTION = 1` (poner 0 para contar solo años completos).
4. **Mes comercial de 30 días** para sueldo proporcional, décimo cuarto y liquidaciones (práctica del Ministerio del Trabajo).
5. **Tope mensual de horas suplementarias = 48 h** (12 h × 4 semanas). Es una validación de entrada, no un cálculo.
6. **Décimo cuarto para jornada parcial**: proporcional a las horas semanales / 40.
7. **Utilidades**: se registran los porcentajes, pero su cálculo anual no está en el MVP.
