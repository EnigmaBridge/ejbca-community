Thymeleaf template engine (http://www.thymeleaf.org/)

- used to send emails

Version 3.0.3

All dependencies:

attoparser-2.0.2.RELEASE.jar	
ognl-3.1.12.jar			
thymeleaf-3.0.3.RELEASE.jar	
unbescape-1.1.4.RELEASE.jar
javassist-3.20.0-GA.jar		
slf4j-api-1.6.6.jar


How to download all dependencies for Thymeleaf:
Create an empty gradle project with the following gradle file: thymeleaf.gradle
Then execute task copyDependencies, all JARs are in dependencies/ folder.
 
