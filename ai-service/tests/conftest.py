"""Synthetic historical procedure fixtures for AI advisory tests.

The fixture data is intentionally fictional and kept in the test suite. It is
not a copy of customer, aircraft, or operational data.
"""

from __future__ import annotations

import copy

import pytest


@pytest.fixture
def flight_control_advisory_payload() -> dict:
    """Target FCC and historical procedure evidence from three LRU families."""
    return {
        "target_lru_id": 900,
        "target_lru_name": "Synthetic Flight Control Computer Variant D",
        "subsystem": "Synthetic Flight Controls",
        "interfaces": ["ARINC429", "Discrete I/O"],
        "protocols": ["ARINC429"],
        "known_fault_scenarios": ["Voltage Drop"],
        "historical_lrus": [
            {
                "lru_id": 101,
                "name": "Synthetic Flight Control Computer Variant A",
                "subsystem": "Synthetic Flight Controls",
                "interfaces": ["ARINC429", "Discrete I/O"],
                "protocols": ["ARINC429"],
                "fault_scenarios": ["Voltage Drop", "Signal Loss"],
                "test_cases": [
                    {
                        "title": "Verify ARINC signal-loss recovery",
                        "objective": "Confirm controlled recovery after synthetic signal interruption.",
                        "fault_scenario": "Signal Loss",
                    },
                    {
                        "title": "Verify voltage-drop alarm",
                        "fault_scenario": "Voltage Drop",
                    },
                ],
            },
            {
                "lru_id": 102,
                "name": "Synthetic Flight Control Computer Variant B",
                "subsystem": "Synthetic Flight Controls",
                "interfaces": ["ARINC429", "Discrete I/O"],
                "protocols": ["ARINC429"],
                "fault_scenarios": ["Signal Loss", "Data Bus Timeout"],
                "test_cases": [
                    {
                        "title": "Verify ARINC signal-loss recovery",
                        "objective": "Confirm controlled recovery after synthetic signal interruption.",
                        "fault_scenario": "Signal Loss",
                    },
                    {
                        "title": "Verify data-bus timeout annunciation",
                        "fault_scenario": "Data Bus Timeout",
                    },
                ],
            },
            {
                "lru_id": 201,
                "name": "Synthetic Power Distribution Unit",
                "subsystem": "Synthetic Power",
                "interfaces": ["Discrete I/O"],
                "protocols": ["CAN"],
                "fault_scenarios": ["Overcurrent"],
                "test_cases": [
                    {
                        "title": "Verify overcurrent isolation",
                        "fault_scenario": "Overcurrent",
                    },
                ],
            },
        ],
    }


@pytest.fixture
def clone_payload(flight_control_advisory_payload):
    """Return independent fixture copies for tests that deliberately mutate input."""

    return lambda: copy.deepcopy(flight_control_advisory_payload)
