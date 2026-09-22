nextflow.enable.dsl=2

params.project = null
params.manifest = null
params.script = "${projectDir}/../examples/adaptive_at8.groovy"
params.qupath = 'QuPath'
params.cpus = 8
params.memory = '16 GB'

process QUPATH_IMAGE {
    tag "$sample_id"
    cpus params.cpus
    memory params.memory
    errorStrategy 'retry'
    maxRetries 2

    input:
    tuple val(sample_id), val(image_name)

    output:
    tuple val(sample_id), path("${sample_id}.done"), emit: done

    script:
    """
    ${params.qupath} script \
      --project '${params.project}' \
      --image '${image_name}' \
      '${params.script}'
    touch '${sample_id}.done'
    """
}

workflow {
    if (!params.project || !params.manifest)
        error "Required: --project project.qpproj --manifest samples.tsv"

    Channel
        .fromPath(params.manifest)
        .splitCsv(header: true, sep: '\t')
        .map { row -> tuple(row.sample_id as String, row.image_name as String) }
        .set { samples }

    QUPATH_IMAGE(samples)
}
