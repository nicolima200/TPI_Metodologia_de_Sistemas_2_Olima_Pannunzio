# Verificación de la Etapa 6 (pruebas y verificación)

Resumen ejecutable de la verificación de preservación del comportamiento: qué se corrió, con qué protocolo, qué resultado dio cada caso y qué evidencia quedó. El detalle completo, con los fragmentos citados y su lectura, está en la Etapa 6 del informe; este documento es la referencia del repositorio.

## Qué se verificó

El sistema completo en los dos extremos: el estado inicial (tag `estado-inicial`) y el resultado de la serie de refactor completa (los siete pull requests integrados). El criterio es el contrato de comportamiento de los casos E1 a E9 definidos antes de tocar código: mismo texto, mismo orden, mismos resultados y mismo estado de la base.

## Protocolo

- Entorno: MySQL 8.4 en localhost (base `tpi-bd-i`), JDK 24, MySQL Connector/J 9.4.0.
- Partida: `sql/datos_prueba.sql` trunca las dos tablas y recarga los tres usuarios de referencia antes de cada corrida.
- Ejecución: la aplicación corre con las entradas del caso redirigidas por stdin, sin intervención humana; la salida estándar y la de error se capturan en archivos separados.
- Estado de la base: un dump de `usuarios` y `credencialesAcceso` antes de la operación y otro después, en las corridas que operan sobre la base.
- Comparación: diferencia de texto entre la corrida del estado inicial y la del refactorizado, normalizando las marcas de tiempo (la fecha de alta y el último cambio de la credencial varían por ejecución y no son parte del contrato).

## Resultado por caso

| Caso | Antes (`estado-inicial`) | Después (serie completa) | Veredicto |
|------|--------------------------|--------------------------|-----------|
| E1 – Alta válida | «Usuario creado con exito.», usuario id 4 en la base | igual | igual |
| E2 – Alta inválida | error de validación con el mismo texto | igual | igual |
| E3 – Listar | admin, usuario_comun y juan.perez | igual | igual |
| E4 – Buscar existente | datos completos del usuario id 4 | igual | igual |
| E5 – Buscar inexistente | «Usuario no encontrado.» | igual | igual |
| E6 – Actualizar | «Usuario actualizado con exito.», email nuevo en la base | igual | igual |
| E7 – Eliminar | «Usuario eliminado (baja logica).», baja lógica de usuario y credencial | igual | igual |
| E8 – Eliminar inexistente | «Error al eliminar usuario: El usuario con id 999 no existe.» | igual | igual |
| E9 – Estado en BD | registro del id 4 con eliminado = 1 (se lee del dump de E7) | igual | igual |
| Sonda `getAll()` de credenciales | `SQLException`: Column 'id' not found | lista con 2 credenciales | corregido (PR 2a) |
| Fallo forzado en actualizar | la escritura del usuario queda confirmada | el rollback deshace ambas escrituras | mejora verificada (PR 3b) |
| Arranque sin configuración | menú y corte sin mensajes de conexión | igual | igual |

Las dos corridas que no se alcanzan desde el menú tienen su propia ejecución:

- **Sonda S5.** `getAll()` de credenciales no tiene flujo en el menú; una clase de una línea la invoca una sola vez. Antes: `java.sql.SQLSyntaxErrorException: Column 'id' not found.` Después: `getAll() -> 2 credenciales`.
- **Fallo forzado (ROLL).** Un trigger de MySQL emite un error propio (`SIGNAL 45000`) sobre los `UPDATE` de `credencialesAcceso`, instalado entre las dos corridas. La corrida A deja al usuario id 4 recién creado; la B instala el trigger y ejecuta la actualización de E6. Antes: el email del usuario queda cambiado y persiste. Después: ambos valores vuelven a su estado de partida.

## Evidencia

Cada corrida deja su transcript de salida y de error; las que operan sobre la base dejan además su par de dumps. En total, 88 archivos:

- 24 corridas: 12 por estado, con los prefijos `antes_` y `despues_`.
- 48 transcripts: `<estado>_<caso>.out.txt` y `.err.txt`.
- 40 dumps: `<estado>_<caso>.pre.sql` y `.post.sql`.
- Casos con corrida propia: `E1` a `E8`, `S5pro`, `ROLL_A`, `ROLL_B` y `CFG_SIN` (arranque sin `db.properties` ni variables de entorno).
- `S5pro` y `CFG_SIN` no dejan dumps: la sonda solo lee por el defecto y el arranque sin configuración no abre conexión.

La muestra de evidencia (las dos versiones de la sonda, los triggers, las salidas del defecto S5 y del fallo forzado, y un par de transcripts) y los scripts del arnés acompañan al informe como anexo. La reproducción requiere MySQL local con la base del proyecto y `db.properties` (ver `db.properties.example`); los scripts leen las credenciales del entorno o de `db.properties`, sin embeberlas. La traza completa de las 24 corridas se conserva fuera de la entrega y puede ampliarse si la cátedra la solicita.
