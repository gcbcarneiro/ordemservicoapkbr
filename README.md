# 🔧 Ordem de Serviço App

App Android completo para gestão de ordens de serviço com orçamento, geração de PDF e envio via WhatsApp.

## ✨ Funcionalidades

- 🔐 Login e cadastro de usuários (Supabase Auth)
- 📋 Criar, editar e excluir ordens de serviço
- 👤 Dados completos do cliente (nome, telefone, e-mail, endereço)
- 🧾 Orçamento com múltiplos itens (quantidade × valor unitário)
- 📊 Atualização de status (Pendente, Aprovado, Em Andamento, Concluído, Cancelado)
- 📄 Geração de PDF profissional
- 💬 Envio do PDF direto pelo WhatsApp
- 📷 Upload de fotos na ordem de serviço
- 🔁 Atualização em tempo real (Supabase Realtime)

---

## 🚀 Como configurar

### 1. Configure o Supabase

1. Acesse [supabase.com](https://supabase.com) e crie um projeto
2. Vá em **SQL Editor** e cole o conteúdo de `supabase/schema.sql`
3. Execute o SQL — ele cria todas as tabelas, políticas RLS e o bucket de storage
4. Vá em **Project Settings → API** e copie:
   - **Project URL** (ex: `https://xyzxyz.supabase.co`)
   - **anon/public key**

### 2. Configure o GitHub

1. Crie um repositório no GitHub
2. Suba todo o código para o repositório:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/SEU_USUARIO/SEU_REPO.git
   git push -u origin main
   ```
3. No repositório, vá em **Settings → Secrets and variables → Actions**
4. Adicione dois secrets:
   - `SUPABASE_URL` → cole sua Project URL
   - `SUPABASE_ANON_KEY` → cole sua anon key

### 3. Baixe o APK

1. Vá na aba **Actions** do seu repositório
2. Aguarde o build terminar (5–10 minutos)
3. Clique no build concluído → **Artifacts** → baixe **OrdemServico-APK**
4. Extraia o `.zip` e instale o `app-debug.apk` no celular
   > ⚠️ Ative "Instalar de fontes desconhecidas" nas configurações do celular

---

## 🧪 Teste local (opcional)

Crie um arquivo `local.properties` na raiz do projeto (nunca suba para o Git):

```properties
sdk.dir=/caminho/para/seu/android/sdk
SUPABASE_URL=https://xyzxyz.supabase.co
SUPABASE_ANON_KEY=sua-chave-aqui
```

---

## 🗂️ Estrutura do projeto

```
app/src/main/java/com/ordemservico/app/
├── MainActivity.kt
├── SupabaseClient.kt
├── data/
│   ├── models/Models.kt
│   └── repository/
│       ├── AuthRepository.kt
│       └── ServiceOrderRepository.kt
├── ui/
│   ├── navigation/AppNavigation.kt
│   ├── screens/
│   │   ├── LoginScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── CreateEditOrderScreen.kt
│   │   └── OrderDetailScreen.kt
│   ├── theme/Theme.kt
│   └── viewmodels/
│       ├── AuthViewModel.kt
│       └── OrderViewModel.kt
└── utils/
    ├── PdfGenerator.kt
    └── WhatsAppHelper.kt
```

## 🛠️ Tecnologias

| Tecnologia | Uso |
|---|---|
| Kotlin + Jetpack Compose | Interface moderna |
| Supabase Auth | Login e cadastro |
| Supabase Postgrest | Banco de dados (PostgreSQL) |
| Supabase Storage | Upload de imagens |
| Supabase Realtime | Atualizações em tempo real |
| Android PdfDocument | Geração de PDF nativa |
| FileProvider + Intent | Compartilhamento via WhatsApp |
| Coil | Carregamento de imagens |
| GitHub Actions | Build automático do APK |
