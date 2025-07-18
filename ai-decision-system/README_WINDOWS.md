# 🤖 Workstation AI Decision System - Windows Guide

Guide complet pour installer et utiliser le système d'IA sur Windows.

## 🚀 Installation Rapide

### **Option 1: Installation Automatique (Recommandée)**

1. **Télécharger Python** (si pas déjà installé)
   - Aller sur https://python.org
   - Télécharger Python 3.8+ pour Windows
   - **IMPORTANT** : Cocher "Add Python to PATH" lors de l'installation

2. **Lancer l'installation automatique**
   ```cmd
   cd workstation-spring\ai-decision-system
   install_windows.bat
   ```

3. **Configurer la base de données**
   - Éditer le fichier `.env` créé
   - Modifier les paramètres de connexion MySQL

4. **Démarrer le système**
   ```cmd
   start_ai_system.bat
   ```

### **Option 2: PowerShell (Plus Avancée)**

```powershell
# Ouvrir PowerShell en tant qu'administrateur
cd workstation-spring\ai-decision-system

# Installation et démarrage en une commande
.\start_ai_system.ps1

# Ou seulement installer les dépendances
.\start_ai_system.ps1 -Install

# Ou mettre à jour les dépendances
.\start_ai_system.ps1 -Update
```

## 📋 Prérequis Windows

### **1. Python 3.8+**
```cmd
# Vérifier l'installation
python --version
pip --version
```

### **2. MySQL Server**
- Installer MySQL Server 8.0+
- Créer une base de données `workstation_db`
- Noter les identifiants de connexion

### **3. PowerShell 5.1+** (pour le script PowerShell)
```powershell
$PSVersionTable.PSVersion
```

## 🔧 Configuration

### **1. Fichier .env**
Créer/modifier le fichier `.env` :
```env
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=votre_mot_de_passe_mysql
DB_NAME=workstation_db

# API Configuration
API_HOST=0.0.0.0
API_PORT=8000
DASHBOARD_PORT=8050
```

### **2. Base de Données**
```sql
-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS workstation_db;
USE workstation_db;

-- Vérifier que les tables existent
SHOW TABLES;
```

## 🎮 Utilisation

### **Démarrage Rapide**
```cmd
# Méthode 1: Batch file
start_ai_system.bat

# Méthode 2: PowerShell
.\start_ai_system.ps1

# Méthode 3: Manuel
python dashboard_api.py
python visualization_dashboard.py
```

### **Accès aux Interfaces**
- **Dashboard IA** : http://localhost:8050
- **API Documentation** : http://localhost:8000/docs
- **Health Check** : http://localhost:8000/api/health

## 🛠️ Dépannage Windows

### **Problème 1: Python non trouvé**
```cmd
# Vérifier PATH
echo %PATH%

# Réinstaller Python avec "Add to PATH" coché
# Ou ajouter manuellement Python au PATH
```

### **Problème 2: Ports déjà utilisés**
```cmd
# Vérifier les ports utilisés
netstat -ano | findstr :8000
netstat -ano | findstr :8050

# Tuer le processus si nécessaire
taskkill /PID <PID> /F
```

### **Problème 3: Erreur de permissions**
```cmd
# Exécuter en tant qu'administrateur
# Ou modifier la politique d'exécution PowerShell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### **Problème 4: Erreur de connexion MySQL**
```cmd
# Vérifier que MySQL est démarré
net start mysql80

# Tester la connexion
mysql -u root -p
```

### **Problème 5: Dépendances manquantes**
```cmd
# Réinstaller les dépendances
pip install -r requirements.txt --force-reinstall

# Ou utiliser un environnement virtuel
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

## 📊 Fonctionnalités du Dashboard

### **Métriques en Temps Réel**
- 📊 **Occupation Actuelle** : Taux d'occupation des postes
- 🏠 **Télétravail** : Pourcentage de télétravail
- ⚠️ **Alertes** : Nombre d'alertes critiques
- 💡 **Recommandations** : Actions suggérées

### **Graphiques Interactifs**
- 📈 **Tendance d'Occupation** : Évolution sur 30 jours
- 🏠 **Tendance Télétravail** : Évolution du télétravail
- 🔮 **Prédictions** : Prévisions sur 30 jours

### **Alertes et Recommandations**
- 🚨 **Alertes Critiques** : Actions immédiates requises
- 💡 **Recommandations** : Suggestions d'optimisation

## 🔄 Mise à Jour

### **Mettre à jour les dépendances**
```cmd
# Méthode 1: PowerShell
.\start_ai_system.ps1 -Update

# Méthode 2: Manuel
pip install -r requirements.txt --upgrade
```

### **Mettre à jour le code**
```cmd
# Si vous utilisez Git
git pull origin main
pip install -r requirements.txt
```

## 📝 Logs et Monitoring

### **Fichiers de Logs**
```
ai-decision-system/
├── logs/
│   ├── ai_system.log
│   └── error.log
```

### **Monitoring en Temps Réel**
```cmd
# Suivre les logs
tail -f logs/ai_system.log

# Ou utiliser PowerShell
Get-Content logs/ai_system.log -Wait
```

## 🚀 Intégration avec votre Projet

### **1. Intégration Angular**
Ajouter dans votre service Angular :
```typescript
@Injectable({
  providedIn: 'root'
})
export class AIDecisionService {
  private apiUrl = 'http://localhost:8000/api';

  getCurrentAnalysis() {
    return this.http.get(`${this.apiUrl}/analysis/current`);
  }

  getRecommendations() {
    return this.http.get(`${this.apiUrl}/recommendations`);
  }
}
```

### **2. Affichage dans le Dashboard**
```typescript
// Dans votre composant dashboard
ngOnInit() {
  this.aiService.getCurrentAnalysis().subscribe(data => {
    this.aiData = data;
    this.updateDashboard();
  });
}
```

## 🎯 Seuils Configurables

### **Modifier les Seuils**
Éditer `config.py` :
```python
TELEWORK_THRESHOLDS = {
    'max_percentage': 60,      # Seuil critique
    'warning_threshold': 50,   # Seuil d'alerte
    'optimal_range': (20, 40)  # Zone optimale
}

RESERVATION_THRESHOLDS = {
    'critical_high': 90,       # Occupation critique
    'warning_high': 80,        # Occupation élevée
    'optimal_range': (70, 85), # Zone optimale
    'warning_low': 60,         # Occupation faible
    'critical_low': 50         # Occupation très faible
}
```

## 🔒 Sécurité Windows

### **Pare-feu Windows**
```cmd
# Autoriser les ports dans le pare-feu
netsh advfirewall firewall add rule name="AI API" dir=in action=allow protocol=TCP localport=8000
netsh advfirewall firewall add rule name="AI Dashboard" dir=in action=allow protocol=TCP localport=8050
```

### **Antivirus**
- Ajouter le dossier `ai-decision-system` aux exclusions
- Autoriser Python et les scripts dans l'antivirus

## 📞 Support Windows

### **Commandes Utiles**
```cmd
# Vérifier l'état des services
Get-Service | Where-Object {$_.Name -like "*python*"}

# Vérifier les processus
tasklist | findstr python

# Nettoyer les processus Python
taskkill /IM python.exe /F
```

### **Logs Système**
```cmd
# Voir les événements Windows
eventvwr.msc

# Filtrer les erreurs Python
Get-EventLog -LogName Application | Where-Object {$_.Source -like "*Python*"}
```

---

**🎉 Votre système d'IA est maintenant prêt sur Windows !**

Pour toute question spécifique à Windows, consultez les logs ou contactez le support. 