
set ::init {
##
##Do not edit
##Generated with tclsh log4j.properties.tcl
##
status=off
dest = err
name = PropertiesConfig
filter.threshold.type = ThresholdFilter
filter.threshold.level = debug
rootLogger.level = ERROR
#rootLogger.appenderRef.stdout.ref = STDERR
}


set ::template {
appender.${id}.type = RollingFile
appender.${id}.name = ${id}
appender.${id}.fileName = ${ramadda.logdir}/${id}.log
appender.${id}.filePattern = ${ramadda.logdir}/${id}-%d{MM-dd-yy-HH-mm-ss}-%i.log.gz
appender.${id}.layout.type = PatternLayout
appender.${id}.layout.pattern = ${pattern}
appender.${id}.policies.type = Policies
appender.${id}.policies.size.type = SizeBasedTriggeringPolicy
appender.${id}.policies.size.size=10MB
appender.${id}.strategy.type = DefaultRolloverStrategy
appender.${id}.strategy.max = 5
logger.${id}.name =${key}
logger.${id}.level = info
logger.${id}.additivity = false
logger.${id}.appenderRef.rolling.ref = ${id}
}


set ::fp [open log4j.properties w]
puts $::fp $::init

proc logger {id key {pattern {[%5p]  %d{ISO8601} %m%n}}} {
    set tmp $::template
    regsub -all {\${id}} $tmp $id tmp
    regsub -all {\${key}} $tmp $key tmp
    regsub -all {\${pattern}} $tmp $pattern tmp
    puts $::fp $tmp
}


#jetty - doesn't seem to be used
#logger jetty org.mortbay
logger ramadda org.ramadda.repository.ramadda {%m%n}
logger access org.ramadda.repository.access {%m%n}
logger zip org.ramadda.repository.output.ZipOutputHandler
logger chat org.ramadda.repository.collab.ChatOutputHandler
logger db org.ramadda.repository.DatabaseManager
logger harvester org.ramadda.repository.harvester.Harvester
logger search org.ramadda.repository.search {%m%n}

close $::fp
