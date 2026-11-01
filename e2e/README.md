# 🧪 Suíte de Testes End-to-End (E2E) - Maestro

Esta pasta contém a suíte de testes de ponta a ponta (**E2E / End-to-End**) automatizados com o [Maestro](https://maestro.mobile.dev/) para o aplicativo Android **CampusHub / URI Eventos** (`br.com.uri.meuprojeto`).

---

## 📁 Estrutura de Arquivos E2E

```
e2e/
├── subflows/
│   └── login_helper.yaml                    # Subfluxo de autenticação reutilizável (Login Direto)
├── 01_create_event_flow.yaml                # Teste E2E 1: Criação e Validação do Formulário de Eventos
├── 02_event_details_subscription_flow.yaml # Teste E2E 2: Consulta de Detalhes e Inscrição
└── README.md                                # Documentação da suíte E2E
```

---

## 🚀 Como Executar os Testes E2E Localmente

* **Executar a Suíte Completa de Testes E2E:**
  ```bash
  maestro test e2e/
  ```

* **Executar Testes E2E Individuais:**
  ```bash
  # Teste E2E 1: Formulário e Publicação de Eventos
  maestro test e2e/01_create_event_flow.yaml

  # Teste E2E 2: Consulta de Detalhes do Evento
  maestro test e2e/02_event_details_subscription_flow.yaml
  ```
