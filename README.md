# CenirApp

CenirApp és una aplicació Android de productivitat gamificada dissenyada per ajudar els usuaris a millorar els seus hàbits d’estudi, concentració i organització mitjançant un sistema de recompenses, nivells i rutes diàries.

---

## Funcionalitats principals

- Gestió completa de tasques amb categories i filtres  
- Mode focus tipus Pomodoro per millorar la concentració  
- Sistema de gamificació amb XP, nivells i assoliments  
- Rutes diàries (streaks) per fomentar la constància  
- Estadístiques d’activitat i progrés  
- Sistema d’assoliments desbloquejables  

---

## Sistema de gamificació

L’aplicació converteix la productivitat en una experiència motivadora:

- XP per tasques completades i sessions de focus  
- Sistema de nivells amb progressió  
- Rutes diàries basades en objectius complerts  
- Missions automàtiques diàries  
- Assoliments amb diferents condicions  

---

## Arquitectura

El projecte segueix una arquitectura MVVM (Model-View-ViewModel):

- Model: Room Database (SQLite)  
- ViewModel: Lògica de negoci i estat de la UI  
- View: Fragments i interfície d’usuari  

També s’utilitzen:

- Room per persistència local  
- LiveData i Flow per observació reactiva  
- WorkManager per tasques en segon pla  

---

## Funcionalitats en segon pla

- Notificacions diàries automàtiques  
- Recordatoris de tasques  
- Reinici de missions i rutes  
- Alertes de rutes en perill  

---

## Tecnologies utilitzades

- Kotlin  
- Android SDK  
- MVVM  
- Room (SQLite)  
- LiveData / Flow  
- WorkManager  
- Material Design  

---

## Objectiu del projecte

Aquest projecte ha estat creat com a exercici personal per aprendre desenvolupament Android modern, arquitectura de software i creació d’aplicacions reals amb sistemes complexos de dades i lògica.

---

## Estat

Projecte funcional en desenvolupament actiu.

---

## Autor

Desenvolupat per Guillem