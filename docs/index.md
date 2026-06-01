
# Overview

## Template

Este repositório contém um modelo de projeto de API REST completo com Spring Boot, Spring Security e testes unitários.

---

## Estrutura do _template_

```text

.azuredevops                    (configurações entrega contínua)
|
├── Dockerfile                  (Utilizado para realizar o build da imagem do Docker da applicação)
├── pipeline-azure.yml          (Definições da pipeline)
├── pull_request_template.md    (Modelo de mensagem para a pull request)
└── README.md                   (Mais detalhes e informações)

.mvn/wrapper
|
├── maven-wrapper.jar           (JAR executável do wrapper do Maven.)
├── maven-wrapper.properties    (Arquivo properties de configuração do wrapper do Maven.)
└── MavenWrapperDownloader.java (Implementação para download do Maven conforme config.)

docs                            (Documentação do componente)
|
├── index.md                    (Página inicial da documentação.)
├── javascripts/                (Script de js incorporados no MkDocs.)
├── diagrams/                   (Diagramas como código.)
├── diagrams.md                 (Exemplos de diagramas como código.)
├── testes-coverage.md          (Como trabalhar com a ferramenta para cobertura de testes.)
├── ci.md                       (Como funciona o processo de integração contínua.)
├── pull-request.md             (Como funciona o processo de pull request.)
├── versioning.md               (Como trabalhar com o versionamento.)
├── quality-gate.md             (Como funciona o portal de qualidade.)  
└── extras.md                   (Informações extras.)

.gitignore                      (Arquivo .gitignore padrão com suporte a maior parte das IDEs.)
mkdocs.yml                      (Configuração do visualizador de documentação.)
mvnw                            (Wrapper do Maven executável para ambientes Unix-like.)
mvnw.cmd                        (Wrapper do Maven executável para ambientes Windows.)
Makefile                        (Conjunto de comandos para auxiliar o desenvolvimento.)
lombok.config                   (Arquivo de configuração do lombok. Veja seção abaixo para mais informações.)
pom.xml                         (Arquivo 'Project Object Model (POM)' do Maven.)

```

---

## Tecnologias do _template_

- [Java Core Lib](https://dev.azure.com/telefonica-vivo-brasil/DevOps/_git/Vivo.Java.Core){:target="_blank"}
- [Lombok Project](https://projectlombok.org/){:target="_blank"}
- [Apache Maven](https://maven.apache.org/){:target="_blank"}
- [Maven Wrapper](https://github.com/takari/maven-wrapper#maven-wrapper){:target="_blank"}
- [Docker](https://docs.docker.com/){:target="_blank"}
- [Sonarqube](https://docs.sonarqube.org/latest/){:target="_blank"}
- [MkDocs](https://www.mkdocs.org/){:target="_blank"}
- [Azure Pipelines](https://azure.microsoft.com/pt-br/services/devops/pipelines/){:target="_blank"}
- [OpenAPI V3](https://www.openapis.org/){:target="_blank"}
- [SpringDoc OpenAPI](https://springdoc.org/){:target="_blank"}

## Antes de começar
Se o você usa alguma IDE como Eclipse, IntelliJ ou VSCode, atente-se ao fato de que o projeto usa o Lombok. O Lombok dá ao projeto a capacidade de ser razoavelmente 
mais legível, já que você escreverá uma quantidade considerável de código a menos.

Se você ainda não conhece o Lombok, visite o [site](https://projectlombok.org/){:target="_blank"} do projeto e veja o vídeo de 4 minutos para entender o projeto. É bem simples!
Todas as [_features_](https://projectlombok.org/features/all){:target="_blank"} do projeto estão bem documentadas.

Para usá-lo com sua IDE favorita, você deverá registrar o _agent_ do Lombok junto à JVM que executa sua IDE, caso contrário, sua IDE não será capaz de reconhecer as _annotations_
do Lombok. Para mais detalhes de como instalá-lo em sua IDE favorita acesse um dos [Guias de instação](https://projectlombok.org/setup/overview){:target="_blank"} do projeto.

## Iniciando

### Configuração (_setup_)

O projeto está equipado com um wrapper do maven, tornando possível que o projeto possa compilar, rodar e executar os testes mesmo sem o maven instalado.
O wrapper irá validar seu ambiente e, caso não encontre a versão do maven configurada no arquivo "_.mvn/wrapper/maven-wrapper.properties_", ele irá baixar
a versão equivalente e então executar o projeto a partir da versão baixada. O uso do wrapper é útil principalmente quando o desenvolvedor quer usar uma versão
do maven para cada projeto sem ter que configurar globalmente o maven.

Após clonar o projeto, acesse o diretório raiz e execute o comando abaixo para realizar a configuração do seu ambiente de trabalho. 
Para mais detalhes acesse o link do _Maven Wrapper_ disponibilizado em [Template Tecnologies](#template-technologies)


### Configurando o ambiente
##### Para Windows
Utilizar o cmd para realizar os comandos, no Power Shell use o comando abaixo para mudar o terminal:
```
> cmd
```
Você precisa verificar se tem o JDK instalado na máquina para rodar os comandos abaixo:
##### Para Windows
```
> java -version
```
 Conforme a resposta você terá que instalar o JDK.
 
 Caso tenha instalado precisa verificar a variável ambiente JAVA_HOME, digite o comando abaixo:
##### Para Windows
```
> echo %JAVA_HOME%
```
O retorno tem que ser o caminho do Java.

### Construindo o projeto (_build_)

Para realizar o _build_ do projeto basta digitar os comandos abaixo:

#### Compiling the project
##### Para Windows
```
> mvnw.cmd compile
```
##### Para Unix-like
```
$ ./mvnw compile
```

#### Running the project
##### Para Windows
```
> mvnw.cmd spring-boot:run
```
##### Para Unix-like
```
$ ./mvnw spring-boot:run
```

!!! note "Running na IDE"
    Não esquece de adicionar o "Run" Maven e no "Command line" spring-boot:run, para rodar o projeto na IDE sem erros.

Caso tudo ocorra bem, basta acessar o endereço [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html){:target="_blank"} e testar a API.

#### Testing the project
##### Para Windows
```
> mvnw.cmd test
```
##### Para Unix-like
```
$ ./mvnw test
```
Ao finalizar a execução dos testes, serão criadas dentro da pasta _target_ outras duas pastas (_jacoco-ut_ e _surefire-reports_) para acompanhamento das estatísticas e métricas da execução dos testes.
Caso use alguma IDE como eclipse, pode-se usar um plugin como o [EclEmma](https://www.eclemma.org/){:target="_blank"} para acompanhamento.

!!! note "Exception Logstah"
    No log irá exibir o erro de "LogstashTcpSocketAppender[LOGSTASH] - Log destination ... connection failed", isso ocorre pois o sua máquina não consegue conectar no servidor.


### Documentation

Para realizar a instalação dos pacotes necessários do [MKDocs](https://www.mkdocs.org/#installation){:target="_blank"}, execute os comandos abaixo:

!!! note "MKDocs"
    O MKDocs depende do [Python](https://www.python.org/){:target="_blank"} para rodar. O [pip](https://pypi.org/project/pip/){:target="_blank"} é o gerenciador de pacotes do Python. Para realizar a instalação do MKDocs, vetifique se o Python está instalado em seu ambiente.


##### Para Windows
```bash
# Para verificar se o Python.
> python -V

# Após instalar o Python, não esqueça de adicionar na variável ambiente "Path" o diretório do Python.

# Comando Windows instalar o MKDocs:
> python -m pip install mkdocs
> python -m pip install mkdocs-diagrams
> python -m pip install mkdocs-awesome-pages-plugin
> python -m pip install mkdocs-mermaid2-plugin
> python -m pip install mkdocs-material-extensions
> python -m pip install plantuml_markdown
> python -m pip install Markdown
> python -m pip install pymdown-extensions
> python -m pip install markdown-inline-graphviz
> python -m pip install mkdocs-techdocs-core
```

##### Para Unix-like
```bash
# Instala todas as deps necessárias para rodar o mkdocs.
$ pip3 install mkdocs
$ pip3 install mkdocs-techdocs-core
$ pip3 install mkdocs-diagrams
$ pip3 install mkdocs-awesome-pages-plugin
$ pip3 install mkdocs-mermaid2-plugin
$ pip3 install mkdocs-material-extensions
$ pip3 install plantuml_markdown
$ pip3 install Markdown
$ pip3 install pymdown-extensions
$ pip3 install markdown-inline-graphviz

# Instala o PlantUML
$ curl -o plantuml.jar -L http://sourceforge.net/projects/plantuml/files/plantuml.1.2020.16.jar/download && echo "c789ace48347c43073232b1458badc5810c01fe8  plantuml.jar" | sha1sum -c - && mv plantuml.jar /opt/plantuml.jar
$ echo $'#!/bin/sh\n\njava -jar '/opt/plantuml.jar' ${@}' >> /usr/local/bin/plantuml
$ chmod +x /usr/local/bin/plantuml
``` 

Inicializa o servidor de documentação MkDocs.

##### Para Windows
```bash
# Para rodar o servidor no Windows:
> python -m mkdocs serve
```

##### Para Unix-like
```bash
# Rodar servidor de documentação mkdocs.
$ mkdocs serve
``` 
Após rodar o servidor do MKDocs, basta acessar o endereço [http://localhost:8000](http://localhost:8000){:target="_blank"}