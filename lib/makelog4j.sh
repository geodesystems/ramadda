#
#We don't run this for now as it results in class  not found errors
#

remove() {
    echo "file: $1"
    zip -q -d log4j-core-2.16.0.jar $1
}

remove org/apache/logging/log4j/core/lookup/JndiLookup.class

remove org/apache/logging/log4j/core/net/JndiManager\$1.class
remove org/apache/logging/log4j/core/net/JndiManager\$JndiManagerFactory.class
remove org/apache/logging/log4j/core/net/JndiManager.class
remove org/apache/logging/log4j/core/util/JndiCloser.class
remove org/apache/logging/log4j/core/selector/JndiContextSelector.class

remove org/apache/logging/log4j/core/appender/mom/JmsAppender.class
remove org/apache/logging/log4j/core/appender/mom/JmsAppender\$1.class
remove org/apache/logging/log4j/core/appender/mom/JmsAppender\$Builder.class
remove org/apache/logging/log4j/core/appender/SmtpAppender.class
remove org/apache/logging/log4j/core/appender/SmtpAppender\$1.class
remove org/apache/logging/log4j/core/appender/SmtpAppender\$Builder.class



