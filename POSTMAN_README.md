# Postman Collection - Order Service API

Coleção Postman para testes da API do Order Service.

## 📦 Arquivos

- `Order-Service.postman_collection.json` - Coleção com todas as requisições
- `Order-Service.postman_environment.json` - Variáveis de ambiente

## 🚀 Como Usar

### 1. Importar no Postman

1. Abra o Postman
2. Clique em **Import** (canto superior esquerdo)
3. Arraste os arquivos `Order-Service.postman_collection.json` e `Order-Service.postman_environment.json`
4. Selecione o ambiente **Order Service - Local** no canto superior direito

### 2. Executar os Testes (Sequência Recomendada)

#### Passo 1: Obter Token
1. Execute a requisição **`1. Authentication > Get Test Token`**
2. O token será automaticamente armazenado na variável `auth_token`

**Nota:** Esta requisição funciona apenas quando a aplicação barrar rodando com o perfil `simple`:
```bash
java -jar target/order-service.jar --spring.profiles.active=simple
```

#### Passo 2: Enviar Mensagem para RabbitMQ
1. Execute a requisição **`2. RabbitMQ - Enviar Mensagem > Send Order Message`**
2. A mensagem será enviada para a fila `orders.incoming.q` através do exchange `orders.incoming.ex`
3. O `external_id` e `correlation_id` serão gerados automaticamente
4. Aguarde alguns segundos para o pedido ser processado

**Configuração Necessária:**
- RabbitMQ Management deve estar rodando na porta 15672
- Credenciais: `guest/guest` (ou ajuste as variáveis `rabbitmq_user` e `rabbitmq_password`)

#### Passo 3: Consultar Pedidos
1. Execute a requisição **`3. Orders - Consultar > List Orders`**
2. Esta requisição lista todos os pedidos com status `AVAILABLE_FOR_B`
3. O primeiro `order_id` e `order_version` serão salvos automaticamente

#### Passo 4: Consultar Pedido Específico
1. Execute a requisição **`3. Orders - Consultar > Get Order by ID`**
2. Esta requisição usa o `order_id` salvo anteriormente
3. A versão do pedido será atualizada na variável `order_version`

#### Passo 5: Confirmar Pedido (Opcional)
1. Execute a requisição **`3. Orders - Consultar > Acknowledge Order`**
2. Esta requisição confirma o recebimento do pedido
3. O header `If-Match` usa a versão salva para controle de concorrência otimista

## 🔧 Variáveis de Ambiente

A coleção usa as seguintes variáveis:

| Variável | Valor Padrão | Descrição |
|----------|-------------|-----------|
| `base_url` | `http://localhost:8080/api/v1` | URL base da API |
| `rabbitmq_host` | `localhost:15672` | Host e porta do RabbitMQ Management |
| `rabbitmq_user` | `guest` | Usuário do RabbitMQ |
| `rabbitmq_password` | `guest` | Senha do RabbitMQ |
| `auth_token` | *(auto)* | Token JWT gerado automaticamente |
| `order_id` | *(auto)* | ID do pedido salvo automaticamente |
| `order_version` | *(auto)* | Versão do pedido salvo automaticamente |
| `external_id` | *(auto)* | External ID gerado automaticamente |
| `correlation_id` | *(auto)* | Correlation ID gerado automaticamente |

## 📝 Exemplo de Fluxo Completo

```
1. Get Test Token → Token armazenado
2. Send Order Message → Mensagem enviada (external_id: EXT-12345)
3. Aguardar 2-3 segundos (processamento assíncrono)
4. List Orders → Ver pedidos disponíveis
5. Get Order by ID → Ver detalhes do pedido específico
6. Acknowledge Order → Confirmar pedido
```

## 🔍 Alternativa: Enviar Mensagem via cURL

Se preferir enviar mensagens diretamente via terminal:

```bash
# Usando o script fornecido
./scripts/send-sample-messages.sh

# Ou via curl diretamente
curl -u guest:guest -X POST \
  http://localhost:15672/api/exchanges/%2F/orders.incoming.ex/publish \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {},
    "routing_key": "order.created",
    "payload": "{\"externalId\":\"EXT-001\",\"correlationId\":\"CORR-001\",\"items\":[{\"productId\":\"PROD-001\",\"productName\":\"Product 1\",\"unitPrice\":10.50,\"quantity\":2}]}",
    "payload_encoding": "string"
  }'
```

## 🐛 Troubleshooting

### Erro 401 Unauthorized
- Verifique se o token foi obtido corretamente
- Execute novamente `Get Test Token`
- Confirme que a aplicação está rodando com perfil `simple` se estiver usando o TestTokenController

### Mensagem não aparece processada
- Verifique os logs da aplicação
- Confirme que o RabbitMQ está rodando
- Verifique se a fila `orders.incoming.q` está criada
- Confirme que o routing key está correto: `order.created`

### Erro ao enviar para RabbitMQ
- Verifique se o RabbitMQ Management está acessível em `http://localhost:15672`
- Confirme as credenciais (guest/guest)
- Verifique se o exchange `orders.incoming.ex` existe

