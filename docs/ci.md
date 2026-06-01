# CI - Integração Contínua

*“Integração Contínua é uma pratica de desenvolvimento de software onde os membros de um time integram seu trabalho frequentemente, geralmente cada pessoa integra pelo menos diariamente – podendo haver múltiplas integrações por dia. Cada integração é verificada por um build automatizado (incluindo testes) para detectar erros de integração o mais rápido possível. Muitos times acham que essa abordagem leva a uma significante redução nos problemas de integração e permite que um time desenvolva software coeso mais rapidamente.”* - **Martin Fowler**

---

## Benefícios

1. **Melhore a produtividade do desenvolvedor**

    A integração contínua ajuda sua equipe a ser mais produtiva ao liberar os desenvolvedores de tarefas manuais e encorajar comportamentos que ajudam a reduzir o número de erros e bugs implantados para os clientes.


2. **Encontre e investigue bugs mais rapidamente**

    Com testes mais frequentes, sua equipe pode descobrir e investigar bugs mais cedo, antes que no futuro os problemas cresçam demais.


3. **Distribua atualizações mais rapidamente**

    A integração contínua ajuda a sua equipe a distribuir atualizações para os clientes mais rapidamente e com maior frequência.

---

## Azure Pipelines

As esteiras da Azure são ativadas pela criação de uma "Pull Request" e após aceitar a PR e realizar o merge com os branchs de integração.

Os arquivos de configuração de construção estão na pasta `.azuredevops/` localizada na raiz do projeto.

### Config

1. Dockerfile - Utilizado para rodar os teste, obter a cobertura e realizar a criação do pacote
2. pipeline-azure.yml - Definições da pipeline
3. pull_request_template - Modelo de mensagem para a pull request
4. README.md - Mais detalhes e informações


### PR Validation

1. Executa dos testes
2. Valida o lint
3. Valida o quality Gate
4. Valida a construção

### Build & Registry

1. Executa dos testes
2. Revalida o lint e quality Gate
4. Realiza a construção do pacote
5. Faz a publicação do artefato


---

## Referências

https://martinfowler.com/articles/continuousIntegration.html