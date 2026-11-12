# Boards 2026-11b (Claude, 2026-11-12)

Open `boards.html` in a browser: Dot on the left, Colour on the right, measurements in the table at the bottom.
Drawn from the tokens in `docs/design/README.md`. **Approved by the owner on 2026-11-12.**

| Board | Card | What comes from data |
|---|---|---|
| 1 ErrorState | ERR1 | What failed (screen/section name), the cause sentence chosen from the error type, the technical line (`HTTP status · exception message · host`). No stack traces, no query strings. |
| 2 LoadingStages | ERR1 | The step names for each flow and the seconds on the current step. Shown only after 2 s of waiting. |
| 3 Recap (+ reader selection colours) | S3-F, RP1 | Previous chapter name, time since it was read, its last three paragraphs. |
| 5 Voices (approved 2026-11-13) | NT3 | Pack size and bytes downloaded, free space, the voice names in the pack, which engine and voice are selected. |
| 4 TabletListenDock (+ read-along highlight) | RP1 | Chapter name, paragraph index and count, speed, timer time left. |

Example copy on the boards (book, chapter and source names, "HTTP 403 · DDoS-Guard") is sample text.
