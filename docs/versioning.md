# Versionando

O versionamento segue o modelo de versão semantica (**[semver](https://semver.org/lang/pt-BR/)**) `(<major>.<minor>.<patch>)`.

## Obter versão atual

Para obter a versão atual do seu projeto, basta ir no `pom.xml` e verificar a chave `<version>`.

## Tabela de exemplos

| REGRA      | ANTES         | DEPOIS        |
|------------|---------------|---------------|
| major      | 1.3.0         | 2.0.0         |  
| minor      | 2.1.4         | 2.2.0         |
| patch      | 4.1.1         | 4.1.2         |

A atualização de versões pode ser feita manualmente no seu `pom.xml` alterando a chage `version`, entretanto, também é possível usar o plugin de versões do maven conforme comandos abaixo.

## Correções

```bash
# antes: 4.1.1
./mvnw versions:set -DnewVersion=4.1.2
# depois: 4.1.2
```

## Implementações

```bash
# antes: 1.1.1
./mvnw versions:set -DnewVersion=1.2.0
# antes: 1.2.0
```

---

## Mudanças de ruptura

```bash
# antes: 1.0.2 
./mvnw versions:set -DnewVersion=2.0.0
# antes: 2.0.0
```

!!! note "Maven Versions Plugin"
    O plugin `versions` do maven possui uma série de outras funcionalidades úteis.
    Caso queira conhecer mais, clique [aqui](https://www.mojohaus.org/versions-maven-plugin/){:target="_blank"} para saber mais sobre o maven versions plugin.