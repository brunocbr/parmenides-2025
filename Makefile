include .env

OUTPUT_DIR=_output
LATEX_DIR=latex
DATA_DIR=data

all: abstracts.tex book

abstracts.tex:
	bb resumos.bb --path $(DATA_DIR) --format latex >$(OUTPUT_DIR)/abstracts.tex

book: abstracts.tex
	cd $(OUTPUT_DIR) && xelatex ../$(LATEX_DIR)/book-of-abstracts.latex

clean:
	rm -f $(OUTPUT_DIR)/abstracts.tex $(OUTPUT_DIR)/book-of-abstracts.pdf \
		$(OUTPUT_DIR)/*.aux $(OUTPUT_DIR)/*.log $(OUTPUT_DIR)/*.out

deploy_pdf: book
	aws s3 cp $(OUTPUT_DIR)/book-of-abstracts.pdf s3://$(AWS_S3_BUCKET_NAME)/IAPS_8_Book_of_Abstracts.pdf


