# esafetyKR
[![](https://github.com/TaYaKi71751/esafetyKR/actions/workflows/main.yml/badge.svg)](https://github.com/TaYaKi71751/esafetyKR/actions/workflows/main.yml)
> ## Config
> ```
> echo "username=$USERNAME \
>       password=$PASSWORD \
>       url=$ESAFETYKOREA_SITE_URL" > ./.env
> ```
> ## Install Dependencies
>> ### Run
>> <pre>
>> <code>
>> pacman -S jdk-openjdk
>> </code>
>> </pre>
>> ### else
>> <pre>
>> <code>
>> pacman -S jdk-openjdk maven
>> </code>
>> </pre>
> ## Run
> <pre>
> <code>
> java -jar ${JAR_FILE_NAME}.jar
> </code>
> </pre>
> ## Package
> <pre>
> <code>
> mvn -B package --file pom.xml
> </code>
> </pre>
