# TicketSystem
Plugin de tickets, denúncias e sugestões para Minecraft.

## Recursos

- Sistema de ticket com acompanhamento e avaliação da staff.
- Sistema de denúncias e sugestões.
- Dados armazenados em banco de dados.
- Mensagens totalmente customizadas.

## Módulos

- Ticket | Ajuda

| Comando | Descrição | Permissão |
| ------ | ------ | ----- |
| ticket help | Ajuda | 
| ajudar \| support | Informa disponibilidade de ajuda | ticketsystem.ticket.staff
| ticket \| ajuda | Envia pedido de ajuda | ticketsystem.ticket.use
| tickets | Lista todos os tickets pendentes | ticketsystem.ticket.staff
| ticket rate | Avaliar ticket | ticketsystem.ticket.staff
| ticket response | Responder ticket | ticketsystem.ticket.staff
| ticket stats | Estatísticas da staff | ticketsystem.ticket.admin
| ticket teleport | Teleportar para o jogador | ticketsystem.ticket.staff
| ticket view | Ver ticket | ticketsystem.ticket.staff

- Report | Denúncia

| Comando | Descrição | Permissão |
| ------ | ------ | ----- |
| report help | Ajuda |
| report \| reportar | Envia uma denúncia | ticketsystem.report.use
| reports | Lista reports pendentes | ticketsystem.report.staff
| report status | Altera o status do report | ticketsystem.report.staff
| report teleport | Teleporta ao jogador reportado | ticketsystem.report.staff
| report view | Ver report | ticketsystem.report.staff

- Suggestion | Sugestão

| Comando | Descrição | Permissão |
| ------ | ------ | ----- |
| suggestion help | Ajuda |
| suggestion \| sugestao | Envia uma sugestão | ticketsystem.suggestion.use
| suggestions | Lista sugestões pendentes | ticketsystem.suggestion.staff
| suggestion response | Responde a sugestão | ticketsystem.suggestion.staff
| suggestion view | Ver sugestão | ticketsystem.suggestion.staff

### Admin
- Recarregar plugin:
```sh
ticketsystem reload ou ts reload
```
Permissão: ```ticketsystem.admin```
