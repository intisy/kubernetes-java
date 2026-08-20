```
KubernetesProvider kubernetesProvider = KubernetesProvider.get();
kubernetesProvider.ensureInstalled();
kubernetesProvider.start();
KubernetesClient kubernetesClient = kubernetesProvider.getClient();
```

Currently supported setups:
  * [x] - Linux root
  * [x] - Linux rootless
  * [x] - Windows Administrator 
  * [x] - Windows non-Administrator 
  * [x] - macOS root 
  * [x] - macOS rootless
