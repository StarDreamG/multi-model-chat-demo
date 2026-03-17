"""Compile-only backend sample for test environment validation."""

from dataclasses import dataclass


@dataclass
class HealthPayload:
    status: str
    version: str


def build_health_payload() -> HealthPayload:
    """Return static metadata without external dependencies."""
    return HealthPayload(status="ok", version="0.1.0")
