PROJECT ?=
MANIFEST ?= samples.tsv
QUPATH ?= QuPath
REFERENCE_JSON ?= fixtures/parity/reference.json
PROMOTED_JSON ?= fixtures/parity/promoted.json

.PHONY: measure estimate resume test parity package

measure:
	nextflow run workflow/main.nf -resume \
		--project "$(PROJECT)" --manifest "$(MANIFEST)" --qupath "$(QUPATH)" \
		--script "$(CURDIR)/examples/exact_at8.groovy"

# Intentionally fails until the exact-parity gate is satisfied and adaptive mode is enabled.
estimate:
	nextflow run workflow/main.nf -resume \
		--project "$(PROJECT)" --manifest "$(MANIFEST)" --qupath "$(QUPATH)" \
		--script "$(CURDIR)/examples/adaptive_at8.groovy"

resume:
	nextflow run workflow/main.nf -resume \
		--project "$(PROJECT)" --manifest "$(MANIFEST)" --qupath "$(QUPATH)"

test:
	@python3 tests/test_scaffold.py

parity:
	@python3 tools/compare_exact_parity.py "$(REFERENCE_JSON)" "$(PROMOTED_JSON)"

package:
	@zip -qr qupath-extension-adaptive-whole-tissue-src.zip . \
		-x '.git/*' 'build/*' '*.zip'
