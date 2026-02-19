"""
Utility functions for data processing and model management
"""

import numpy as np
from typing import List, Dict, Any
import logging

logger = logging.getLogger(__name__)

def normalize_data(data: List[float], method: str = "minmax") -> List[float]:
    """Normalize data using min-max or z-score"""
    if not data:
        return []
    
    arr = np.array(data)
    
    if method == "minmax":
        min_val = np.min(arr)
        max_val = np.max(arr)
        if max_val == min_val:
            return [0.5] * len(data)
        normalized = (arr - min_val) / (max_val - min_val)
    elif method == "zscore":
        mean = np.mean(arr)
        std = np.std(arr)
        if std == 0:
            return [0.0] * len(data)
        normalized = (arr - mean) / std
    else:
        return data
    
    return normalized.tolist()

def create_sequences(data: List[List[float]], seq_length: int = 60) -> np.ndarray:
    """Create sequences for LSTM input"""
    sequences = []
    for i in range(len(data) - seq_length + 1):
        seq = data[i:i + seq_length]
        sequences.append(seq)
    return np.array(sequences)

def calculate_sharpe_ratio(returns: List[float], risk_free_rate: float = 0.02) -> float:
    """Calculate Sharpe ratio"""
    if not returns or len(returns) < 2:
        return 0.0
    
    excess_returns = [r - risk_free_rate/252 for r in returns]  # Daily risk-free rate
    mean_excess = np.mean(excess_returns)
    std_excess = np.std(excess_returns)
    
    if std_excess == 0:
        return 0.0
    
    return (mean_excess / std_excess) * np.sqrt(252)  # Annualized

def format_large_number(num: float) -> str:
    """Format large numbers (e.g., 1500000 -> 1.5M)"""
    if num >= 1e9:
        return f"{num/1e9:.2f}B"
    elif num >= 1e6:
        return f"{num/1e6:.2f}M"
    elif num >= 1e3:
        return f"{num/1e3:.2f}K"
    else:
        return f"{num:.2f}"