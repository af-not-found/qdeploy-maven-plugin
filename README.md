# qdeploy-maven-plugin
qdeploy-maven-plugin is a maven plugin to deploy war file quickly.

### Overview
qdeploy-maven-plugin requires a daemon web application named [qdeploy-maven-webapp](https://github.com/af-not-found/qdeploy-maven-webapp).

![img](http://static.afnf.net/sstatic/qdeploy-maven-plugin.png?v=1)

### Configuration

#### plugin side
- QDEPLOY_KEY (system property or environment value)
  - String for authentication.
- pom.xml
````xml
<plugin>
    <groupId>net.afnf</groupId>
    <artifactId>qdeploy-maven-plugin</artifactId>
    <version>1.0.1</version>
    <configuration>
      <webappDir>target/myapp</webappDir>
      <finalName>myapp</finalName>
      <deployUrl>http://localhost:8080/qdeploy</deployUrl>
    </configuration>
</plugin>
````

#### webapp side
- QDEPLOY_WARDIR (system property or environment value)
  - Path to directly of war file, such as /usr/local/jetty/webapps/, /usr/local/tomcat/webapps/. 
- QDEPLOY_KEY (system property or environment value)
  - String for authentication.

