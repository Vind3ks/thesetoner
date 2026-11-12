# Files for the FAForever/gitops-stack PR

These follow `apps/faf-qai`, the other IRC bot, which uses the same `IRC_*` settings and the same fixed-user login. The Recreate strategy, init container and volume follow `apps/faf-daily`.

Changes in gitops-stack:

1. **New app:** copy `apps/faf-setoner/` to `apps/faf-setoner/`. The ApplicationSet picks up every `apps/*` folder.
2. **Volume:** add the entry from `cluster-storage-values.snippet.yaml` to `cluster/storage/values.yaml` under `managedStorages`.
3. **IRC login:** add `FAF_IRC_FIXED_USERS_THESETONER: "banana"` to `apps/faf-user-service/templates/local-secret.yaml`, next to `FAF_IRC_FIXED_USERS_QAI`.

Secrets in Infisical (prod and test). Both use the same freshly generated password:

- faf-user-service: `FAF_IRC_FIXED_USERS_THESETONER=<password>`
- faf-setoner: `IRC_PASS=<password>`

On the faf-setoner repo, add the GitHub secrets `DOCKER_USERNAME` and `DOCKER_PASSWORD` so CI can push `faforever/faf-setoner:main`.
