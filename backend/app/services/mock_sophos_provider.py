from typing import List, Dict, Any

class MockSophosProvider:
    """
    High-fidelity offline mock adapter for Sophos Central APIs.
    Provides realistic enterprise SOC telemetry adhering to official Sophos schemas.
    """

    def __init__(self):
        self.endpoints = [
            {
                "id": "ep-us-prod-01",
                "hostname": "SEC-CORP-DC01",
                "ip_addresses": ["10.14.0.5", "192.168.10.5"],
                "os": "Windows Server 2022 Datacenter",
                "health": "SUSPICIOUS",
                "isolated": False,
                "tamper_protection": True,
                "last_seen_at": "Just now",
                "is_server": True,
                "agent_version": "2026.2.1",
                "group_name": "Domain Controllers"
            },
            {
                "id": "ep-us-prod-02",
                "hostname": "FIN-WS-4091",
                "ip_addresses": ["10.14.2.144"],
                "os": "Windows 11 Enterprise 24H2",
                "health": "BAD",
                "isolated": True,
                "tamper_protection": True,
                "last_seen_at": "2 mins ago",
                "is_server": False,
                "agent_version": "2026.2.0",
                "group_name": "Finance Workstations"
            },
            {
                "id": "ep-us-prod-03",
                "hostname": "ENG-UBUNTU-DEV08",
                "ip_addresses": ["10.14.5.88"],
                "os": "Ubuntu Linux 24.04 LTS",
                "health": "GOOD",
                "isolated": False,
                "tamper_protection": True,
                "last_seen_at": "5 mins ago",
                "is_server": False,
                "agent_version": "2026.1.4",
                "group_name": "Engineering Workstations"
            },
            {
                "id": "ep-us-prod-04",
                "hostname": "APP-K8S-NODE-03",
                "ip_addresses": ["10.14.8.21"],
                "os": "Red Hat Enterprise Linux 9.4",
                "health": "GOOD",
                "isolated": False,
                "tamper_protection": True,
                "last_seen_at": "1 min ago",
                "is_server": True,
                "agent_version": "2026.2.0",
                "group_name": "Kubernetes Cluster Nodes"
            }
        ]

        self.alerts = [
            {
                "id": "alt-89102",
                "severity": "CRITICAL",
                "category": "Ransomware / Cryptoguard",
                "description": "CryptoGuard detected and reverted malicious file encryption by process 'vssadmin.exe'",
                "endpoint_hostname": "FIN-WS-4091",
                "endpoint_id": "ep-us-prod-02",
                "detected_at": "12 mins ago",
                "status": "OPEN",
                "product": "Sophos Intercept X",
                "threat_name": "Troj/Ransom-J",
                "path": "C:\\Users\\FinanceUser\\AppData\\Local\\Temp\\updater.exe"
            },
            {
                "id": "alt-89103",
                "severity": "HIGH",
                "category": "Credential Theft",
                "description": "LSASS memory dumping attempted via unapproved debug privilege escalation",
                "endpoint_hostname": "SEC-CORP-DC01",
                "endpoint_id": "ep-us-prod-01",
                "detected_at": "24 mins ago",
                "status": "OPEN",
                "product": "Sophos Intercept X with EDR",
                "threat_name": "Mimikatz Memory Harvester",
                "path": "C:\\Windows\\System32\\lsass.exe"
            },
            {
                "id": "alt-89104",
                "severity": "MEDIUM",
                "category": "Exploit Prevention",
                "description": "ROP gadget detected in browser child process; payload blocked before execution",
                "endpoint_hostname": "ENG-UBUNTU-DEV08",
                "endpoint_id": "ep-us-prod-03",
                "detected_at": "1 hour ago",
                "status": "ACKNOWLEDGED",
                "product": "Sophos Intercept X",
                "threat_name": "Exp/ROP-Chrome",
                "path": "/opt/google/chrome/chrome"
            }
        ]

        self.detections = [
            {
                "id": "det-301",
                "severity": "CRITICAL",
                "title": "Suspicious PowerShell In-Memory Assembly Loading",
                "category": "Execution (T1059.001)",
                "target_endpoint": "FIN-WS-4091",
                "endpoint_id": "ep-us-prod-02",
                "risk_score": 94,
                "mitre_techniques": ["T1059.001", "T1027", "T1003.001"],
                "detected_at": "15 mins ago",
                "status": "Active",
                "process_path": "powershell.exe -NoP -NonI -W Hidden -Enc SUVY...",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            },
            {
                "id": "det-302",
                "severity": "HIGH",
                "title": "NTDS.dit Directory Database Shadow Copy Extraction",
                "category": "Credential Access (T1003.003)",
                "target_endpoint": "SEC-CORP-DC01",
                "endpoint_id": "ep-us-prod-01",
                "risk_score": 88,
                "mitre_techniques": ["T1003.003", "T1490"],
                "detected_at": "30 mins ago",
                "status": "Pending Escalation",
                "process_path": "ntdsutil.exe \"ac i ntds\" \"ifm\" \"create full C:\\temp\" q q",
                "sha256": "c5d806a6659f81bb3b6481bb2bb6a9a7a67119ffb264906f376cf9ad0106ea4d"
            }
        ]

        self.cases = [
            {
                "id": "case-2026-0042",
                "case_number": "XDR-0042",
                "title": "Finance Workstation Ransomware Outbreak Containment",
                "status": "IN_PROGRESS",
                "priority": "URGENT",
                "assignee": "Lead SOC Responder (You)",
                "detection_count": 4,
                "summary": "Cryptoguard telemetry combined with in-memory PowerShell invocation on FIN-WS-4091. Isolation enacted successfully.",
                "impacted_endpoints": ["FIN-WS-4091", "SEC-CORP-DC01"]
            },
            {
                "id": "case-2026-0039",
                "case_number": "XDR-0039",
                "title": "Suspicious Domain Controller Replication Sync Probe",
                "status": "NEW",
                "priority": "HIGH",
                "assignee": "Unassigned",
                "detection_count": 2,
                "summary": "Anomalous DCSync RPC calls detected from non-DC subnet against SEC-CORP-DC01.",
                "impacted_endpoints": ["SEC-CORP-DC01"]
            }
        ]

    def get_endpoints(self) -> List[Dict[str, Any]]:
        return self.endpoints

    def get_alerts(self) -> List[Dict[str, Any]]:
        return self.alerts

    def get_detections(self) -> List[Dict[str, Any]]:
        return self.detections

    def get_cases(self) -> List[Dict[str, Any]]:
        return self.cases

    def isolate_endpoint(self, endpoint_id: str) -> bool:
        for ep in self.endpoints:
            if ep["id"] == endpoint_id:
                ep["isolated"] = True
                ep["health"] = "BAD"
                return True
        return False

    def restore_endpoint(self, endpoint_id: str) -> bool:
        for ep in self.endpoints:
            if ep["id"] == endpoint_id:
                ep["isolated"] = False
                ep["health"] = "GOOD"
                return True
        return False

mock_provider = MockSophosProvider()
