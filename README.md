# Parmenides SP '2025 Book of Abstracts Generator

This repository contains the tools to generate the Book of Abstracts for the conference "The language of being, the language of nature: Parmenides and hist receptions", and manage Google Drive presentation folders.

## Requirements

To generate the Book of Abstracts, you will need the following tools installed on your system:

- [Babashka](https://github.com/babashka/babashka): A fast, expressive, and feature-rich shell/scripting environment for Clojure.
- [Pandoc](https://pandoc.org/): A universal document converter.
- LaTeX with XeLaTeX: For typesetting the PDF document.

## Setup

1. Clone this repository to your local machine.
2. Create a `.env` file in the root of the repository, exporting `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, with your AWS credentials. Provide bucket and file name information with `AWS_S3_BUCKET_NAME` and `PDF_TARGET_NAME`.

### Google Drive Links

To generate the links to subfolders on a shared Drive folder, one per author name, ensure you have the following:

1. **Google Cloud Project**: Create a Google Cloud project, enable the Google Drive API, and create OAuth2 credentials.
2. **Generate `credentials.json`**:
   - Go to the [Google Cloud Console](https://console.cloud.google.com/).
   - Create a new project or select an existing project.
   - Navigate to the **API & Services > Credentials** page.
   - Click on **Create Credentials** and select **OAuth client ID**.
   - Configure the OAuth consent screen if prompted.
   - Choose **Desktop app** as the application type and click **Create**.
   - Download the `credentials.json` file and save it in the same directory as the script.
 3. Set `GOOGLE_DRIVE_FOLDER_ID` in your `.env` file to the corresponding folder id.

### Example `.env` file

```sh
# AWS Configuration
export AWS_DEFAULT_REGION=sa-east-1
export AWS_ACCESS_KEY_ID=<your access key>
export AWS_SECRET_ACCESS_KEY=<your secret>

# S3 Bucket Name
export AWS_S3_BUCKET_NAME=iaps-8

# Target PDF File Name
export PDF_TARGET_NAME=IAPS_8_Book_of_Abstracts.pdf

# Google Drive Folder ID
export GOOGLE_DRIVE_FOLDER_ID=<the folder id>
```

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

To create a Word Document with the Program:

```sh
make program
```

This will create `program.docx`, which can be used for conference.

Before operating with the Google API, you have to get authorization:

```sh
make google_authorization
```

This will create or refresh the token stored in `token.json`.

To create Google Drive subfolders using the author names:

```sh
make create_drive_subfolders
```

This will create the subfolders using the Google API.

To generate the links database for the author subfolders:

```sh
make generate_drive_links
```

This will create a database containing the links to the author subfolders, which will be used if available in the generation of the Book of Abstracts.

After the subfolders are created, you may send invitations to the authors:

```sh
make send_drive_invitations
```

This will send invitations to the author email addresses with their respectives links, granting write access to a subfolder.

## Contributing

Contributions to this project are welcome. If you find any issues or have suggestions for improvements, please open an issue or submit a pull request.

---
Maintained by Bruno Conte
