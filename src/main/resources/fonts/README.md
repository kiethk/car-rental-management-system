# Fonts for contract PDFs

`OpenHtmlToPdfContractGenerator` embeds these TrueType fonts so Vietnamese diacritics render in the PDF:

- `NotoSans-Regular.ttf`
- `NotoSans-Bold.ttf`

Noto Sans is licensed under the SIL Open Font License 1.1, which allows bundling it with the application.
Keep `OFL.txt` next to the font files.

To use another font, set `CONTRACT_FONT_REGULAR` / `CONTRACT_FONT_BOLD`
(for example `file:C:/Windows/Fonts/arial.ttf`). Do not commit fonts whose licence forbids redistribution, such as Arial.
