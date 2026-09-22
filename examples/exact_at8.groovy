import org.sangha.qupath.adaptive.scripting.AdaptiveMeasurementScripts

def result = AdaptiveMeasurementScripts.exact("AT8 Classifier")
println "EXACT_RESULT=" + result
println "positive_area_um2=" + result.positiveAreaMicrons2()
println "negative_area_um2=" + result.negativeAreaMicrons2()
println "positive_percent=" + result.positivePercent()
println "chunks=" + result.sampledChunks() + "/" + result.totalChunks()
println "failed=" + result.failedChunks()
