FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/mymusic-backend-1.0.0.jar app.jar

# 音频/封面文件存放目录，通过卷持久化
ENV UPLOAD_DIR=/data/uploads
VOLUME ["/data/uploads"]

# 通过 JAVA_OPTS 环境变量控制 JVM 内存（1G 服务器建议 -Xmx384m）
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
