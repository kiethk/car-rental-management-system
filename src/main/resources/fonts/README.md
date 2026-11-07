# Fonts for contract PDFs

`OpenHtmlToPdfContractGenerator` embeds these TrueType fonts so Vietnamese diacritics render in the PDF:

- `LiberationSerif-Regular.ttf`
- `LiberationSerif-Bold.ttf`

Liberation Serif is metric-compatible with Times New Roman (same letter widths, so the layout is identical)
and covers all Vietnamese letters. It is licensed under the SIL Open Font License 1.1, which allows bundling it
with the application. Keep `OFL-LiberationSerif.txt` next to the font files.

`NotoSans-*.ttf` and `OFL.txt` are the previous sans-serif choice and are no longer used by default.

To use the real Times New Roman on your own machine, set `CONTRACT_FONT_REGULAR` / `CONTRACT_FONT_BOLD`
(for example `file:C:/Windows/Fonts/times.ttf` and `file:C:/Windows/Fonts/timesbd.ttf`).
Do not commit fonts whose licence forbids redistribution, such as Times New Roman or Arial.
