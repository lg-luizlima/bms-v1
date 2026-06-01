# Quality Gate

O Sonarqube é o portal de qualidade oficial utilizado pela corporação. Ele está integrado no processo de CI de nossa esteira em dois momentos, a primeira analise ocorre assim que é aberta uma "pull request", a segunda é após o Merge com o branch de integração (develop, release e master/main).

Você também pode executar o sonarqube diretamente de seu ambiente local.

Para executar no seu ambiente local com os comandos abaixo, defina as variáveis de ambiente necessárias para apontar e autenticar no servidor

```bash
export SONARQUBE_URL=http://sonar-devops.redecorp.br/
export SONARQUBE_TOKEN="TOKEN GERADO PELO SONAR"
```

!!! note "Como gerar meu token?" 
    *O token é gerado pela interface web do sonarqube, acesse a [geração de token](http://sonar-devops.redecorp.br/account/security/){:target="_blank"}, preencha o campo "Enter token name" e clique em "Generate".*

## Sonar-Scanner Maven

1. No diretório raiz do projeto, execute o comando abaixo:
    ```bash
    ./mvnw clean verify sonar:sonar \
           -Dsonar.login="$SONARQUBE_TOKEN" \
           -Dsonar.host.url="$SONARQUBE_URL"
    ```

## Sonar-Scanner Docker

1. Baixe a imagem de docker oficial do sonar-scanner

    ```bash
    docker pull sonarsource/sonar-scanner-cli
    ```

2. Executando o sonar-scanner:
    
    No diretório raiz do seu projeto, execute os comandos na sequência abaixo:

      1. 
    ```bash
    ./mvnw clean verify
    ```

      2. 
    ```bash
    export APP_VERSION=$(docker run --rm --log-driver none --entrypoint xq -v $(pwd)/pom.xml:$(pwd)/pom.xml -w $(pwd) linuxserver/yq -r .project.version pom.xml)
    export APP_NAME=$(docker run --rm --log-driver none --entrypoint xq -v $(pwd)/pom.xml:$(pwd)/pom.xml -w $(pwd) linuxserver/yq -r .project.artifactId pom.xml)
    ```

      3. 
    ```bash
    docker run \
        --rm \
        --user="$(id -u):$(id -g)" \
        -e SONAR_HOST_URL="${SONARQUBE_URL}" \
        -e SONAR_LOGIN="${SONARQUBE_TOKEN}" \
        -e SONAR_PROJECT_BASE_DIR=$(pwd) \
        -v $(pwd):$(pwd) \
        sonarsource/sonar-scanner-cli \
            -Dproject.settings=sonar-project.properties \
            -Dsonar.log.level=DEBUG \
            -Dsonar.projectKey="$APP_NAME" \
            -Dsonar.projectVersion="$APP_VERSION" \
            -Dsonar.sources="src/main/java/" \
            -Dsonar.java.binaries="target/classes" \
            -Dsonar.java.test.binaries="target/test-classes" \
            -Dsonar.language="java" \
            -Dsonar.coverage.jacoco.xmlReportPaths="target/jacoco-ut/jacoco.xml"
    ```

## Sonar-Scanner VSCode

Você pode adicionar a extenção **SonarLint (sonarsource.sonarlint-vscode)** em seu VSCode e já integrar sua workspace com o sonarqube.

Após baixar o plugin adicione em seu user settings, não esqueça de fornecer seu token pessoal, este token abaixo é inválido e só a titulo de demonstração:

```json
    "sonarlint.connectedMode.connections.sonarqube": [
        { 
            "serverUrl": "http://sonar-devops.redecorp.br/", 
            "token": "TOKEN GERADO PELO SONAR" }
    ]
```
