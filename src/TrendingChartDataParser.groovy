import org.jfree.data.time.TimeSeries
import org.jfree.data.time.FixedMillisecond

class TrendingChartDataParser {
    def config
    def nl = System.getProperty('line.separator')

    File logFile
    def fileLogging
    def dataFileName
    def logFileName
    def debug

    enum pidDataTitles {
        timestamp(1), systemUlimit(1), pid(1), pidName(2), pidOpenFileDescriptors(3), pidSoftLimit(4), pidHardLimit(5)
        pidDataTitles(int value) { this.value = value }
        private final int value
        public int value() { return value }
    }

    public TrendingChartDataParser(def config) {
        this.config = config
        initialize()
    }

    public def initialize() {
        dataFileName = config.get("dataFile")
        logFileName = config.get("logFile", "trendingChartData.log")
        debug = Boolean.valueOf(config.get("option.debug", false))
        fileLogging = Boolean.valueOf(config.get("option.fileLogging", true))
        if (fileLogging) {
            logFile = new File(logFileName)
            logFile.createNewFile()
            if (!logFile.exists()) {
                fileLogging = false;
                log "'Couldn't create/open " + logFileName + ". Logging to System Console instead.'"
            }
        }

        config.each {
            log(it.key + "->" + it.value)
        }
    }

    def parseDatafile() {
        def pids = [:]
        File inputfile = new File(dataFileName);
        if (!inputfile.exists()) {
            log "'" + dataFileName + "'" + " does not exist"
            return null
        } else {
            def timestampRegEx = config.get("parser.matcher.timestampline.regex", ".*?([a-zA-z]{3}?\\s+[a-zA-z]{3}?\\s+[0-9]{1,2}?\\s+[0-9]{1,2}?:[0-9]{1,2}?:[0-9]{1,2}?\\s+[a-zA-z]{3}?\\s+[0-9]{4}?).*?")
            def systemUlimitRegEx = config.get("parser.matcher.ulimitline.regex", ".*?ULIMIT:\\s*([0-9]+).*?")
            def pidDataRegExV1 = config.get("parser.matcher.ofhline.v1.regex", ".*?([0-9]+)\\s+\\/([^\\s]*)\\s+([0-9]+).*?")
            def pidDataRegExV2 = config.get("parser.matcher.ofhline.v2.regex", ".*?([0-9]+)\\s+\\/(.*)\\s+Open Files\\s*=\\s*([0-9]+)\\s*::\\s*Soft Limit\\s*=\\s*([0-9]+)\\s*::\\s*Hard Limit\\s*=\\s*([0-9]+).*?")
            def pidData = [:]
            inputfile.each { line ->
                def timestampMatcher = (line =~ timestampRegEx)
                if (timestampMatcher != null && timestampMatcher.size() > 0) {
                    pidData = [:]
                    pidData."timestamp" = timestampMatcher[0][pidDataTitles.timestamp.value()]
                    logDebug ""
                    logDebug "Timestamp=" + pidData."timestamp"
                }
                def systemUlimitMatcher = (line =~ systemUlimitRegEx)
                if (systemUlimitMatcher != null && systemUlimitMatcher.size() > 0) {
                    pidData."systemUlimit" = systemUlimitMatcher[0][pidDataTitles.systemUlimit.value()]
                    logDebug "System uLimit=" + pidData."systemUlimit"
                }

                def pidDataMatcher = (line =~ pidDataRegExV1)
                if (pidDataMatcher == null || pidDataMatcher.size() == 0) {
                    pidDataMatcher = (line =~ pidDataRegExV2)
                }
                if (pidDataMatcher != null && pidDataMatcher.size() > 0) {
                    pidData."pid" = pidDataMatcher[0][pidDataTitles.pid.value()]
                    pidData."pidOpenFileDescriptors" = pidDataMatcher[0][pidDataTitles.pidOpenFileDescriptors.value()]
                    pidData."pidName" = pidDataMatcher[0][pidDataTitles.pidName.value()]
                    pidData."pidSoftLimit" = pidDataMatcher[0][pidDataTitles.pidSoftLimit.value()]
                    pidData."pidHardLimit" = pidDataMatcher[0][pidDataTitles.pidHardLimit.value()]
                    logDebug "PID OpenFileDescriptors=" + pidData."pidOpenFileDescriptors"
                    logDebug "PID Name=" + pidData."pidName"
                    logDebug "OFH Soft Limit=" + pidData."pidSoftLimit"
                    logDebug "OFH Hard Limit=" + pidData."pidHardLimit"
                    logDebug "-"

                    TimeSeries pid
                    if (pids.containsKey(pidData."pid")) {
                        pid = pids.get(pidData."pid")
                    } else {
                        pid = new TimeSeries("Process ID (" + pidData."pid" + ")", FixedMillisecond.class);
                    }
                    pid.add(new FixedMillisecond(new Date(pidData."timestamp")), pidData."pidOpenFileDescriptors".toDouble())
                    pids.put(pidData."pid", pid)
                }
            }
        }
        return pids
    }

    def logDebug(def message) {
        log(message, "debug")
    }

    def log(def message, def logLevel = "info") {
        switch (logLevel) {
            case "debug":
                if (debug) {
                    if (fileLogging) {
                        logFile << "Debug: " + message + nl
                    } else {
                        println("Debug: " + message)
                    }
                }
                break
            default:
                if (fileLogging) {
                    logFile << "Info: " + message + nl
                } else {
                    println("Info: " + message)
                }
                break
        }
    }

}
