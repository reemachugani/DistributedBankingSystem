DistributedBankingSystem
========================
Client side

- Menu
1. Prompt for....

- Send serialised data to server (request protocol)


Server side
- on startup read account num and account details
- Deserialise data
- Check option - create, update, delete, monitor for time duration
Add 2 methods - check balance (idempotent)
Adding amount repeatedly on request msg failure (non-idempotent)

- fault tolerance (same machine, same initial state, final state) - on request msg failure

[optionName] , [arguments,]

