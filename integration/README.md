# 🧪 Suíte de 10 Testes de Integração Automatizados - Maestro

Esta pasta contém a suíte de **10 Testes de Integração** individuais e focados para o aplicativo **CampusHub / URI Eventos** (`br.com.uri.meuprojeto`).

---

## 📁 Arquivos da Suíte de Integração

```
integration/
├── subflows/
│   └── login_helper.yaml                    # Subfluxo de autenticação reutilizável
├── 01_login_validation_flow.yaml            # Teste 01: Validação de formulário de login vazio
├── 02_forgot_password_dialog_flow.yaml       # Teste 02: Modal e envio de e-mail de recuperação de senha
├── 03_profile_update_flow.yaml              # Teste 03: Edição e atualização de nome no perfil
├── 04_home_text_search_flow.yaml            # Teste 04: Pesquisa textual dinâmica na lista de eventos
├── 05_home_category_filter_flow.yaml        # Teste 05: Filtro por categoria no Spinner
├── 06_create_event_validation_flow.yaml     # Teste 06: Trava de segurança para limite de vagas inválido (<= 0)
├── 07_date_time_picker_flow.yaml            # Teste 07: Interação com seletores nativos de Data e Hora
├── 08_favorite_toggle_flow.yaml             # Teste 08: Alternância de favorito no card de evento
├── 09_comments_bottom_sheet_flow.yaml       # Teste 09: Abertura do BottomSheet e envio de comentário
├── 10_creator_toggle_status_flow.yaml      # Teste 10: Painel de gestão e alternância de status pelo criador
└── README.md                                # Documentação da suíte
```

---

## 🚀 Como Executar os Testes de Integração

* **Executar TODOS os 10 Testes de Integração em Sequência:**
  ```bash
  maestro test integration/
  ```

* **Executar Testes Individuais (Exemplo):**
  ```bash
  maestro test integration/01_login_validation_flow.yaml
  maestro test integration/03_profile_update_flow.yaml
  ```
