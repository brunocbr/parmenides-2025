# IAPS 8th Conference Book of Abstracts Generator

This repository contains the tools to generate the Book of Abstracts for the 8th Conference of the International Association for Presocratic Studies (IAPS).

## Requirements

To generate the Book of Abstracts, you will need the following tools installed on your system:

- [Babashka](https://github.com/babashka/babashka): A fast, expressive, and feature-rich shell/scripting environment for Clojure.
- [Pandoc](https://pandoc.org/): A universal document converter.
- LaTeX with XeLaTeX: For typesetting the PDF document.

## Usage

To generate the Book of Abstracts in PDF format, use the following command:

```sh
make book
```

This command will compile the abstracts and generate the PDF file.

To upload the generated PDF to AWS S3, use the following command:

```sh
make deploy_pdf
```

This will upload the PDF to the specified AWS S3 bucket.

---
Maintained by Bruno Conte
