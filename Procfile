# JVM options sized for an S container (256 MB), see README "Costs". JAVA_OPTS comes last and overrides them.
web: java -Dserver.port=$PORT -XX:+UseSerialGC -Xmx96m -XX:MaxMetaspaceSize=110m -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=16m -Xss512k -XX:+ExitOnOutOfMemoryError $JAVA_OPTS -jar backend/build/libs/openvesselphotos.jar
