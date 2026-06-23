import csv
from datetime import datetime, timedelta

def generate_test_data():
    filename = 'test_trades.csv'
    headers = ['symbol', 'instrumentType', 'tradeType', 'quantity', 'price', 'tradeDate', 'tradeTime', 'exchange']
    
    # Base time for chronological progression
    base_time = datetime(2026, 6, 18, 9, 15, 0) # Changed to 16th to generate new hashes

    trades = []

    def add_trade(symbol, inst_type, t_type, qty, price, minutes_offset):
        trade_time = base_time + timedelta(minutes=minutes_offset)
        trades.append([
            symbol, 
            inst_type, 
            t_type, 
            qty, 
            f"{price:.2f}", 
            trade_time.strftime('%Y-%m-%d'), 
            trade_time.strftime('%Y-%m-%dT%H:%M:%S'), 
            'NSE'
        ])

    # ---------------------------------------------------------
    # SCENARIO 1: The "Simple Match"
    # Buy 100 shares, Sell 100 shares later at a profit.
    # Expected Result: 1 SELL with full realized P&L.
    # ---------------------------------------------------------
    add_trade('HDFCBANK', 'EQ', 'BUY', 100, 1500.00, 0)
    add_trade('HDFCBANK', 'EQ', 'SELL', 100, 1520.00, 30)

    # ---------------------------------------------------------
    # SCENARIO 2: The "Partial Sell"
    # Buy 100 shares. Sell 40. Sell 60.
    # Expected Result: First SELL calculates P&L for 40. 
    # Second SELL calculates P&L for 60. Remaining qty = 0.
    # ---------------------------------------------------------
    add_trade('RELIANCE', 'EQ', 'BUY', 100, 2400.00, 60)
    add_trade('RELIANCE', 'EQ', 'SELL', 40, 2410.00, 90)
    add_trade('RELIANCE', 'EQ', 'SELL', 60, 2390.00, 120)

    # ---------------------------------------------------------
    # SCENARIO 3: The "Complex FIFO Aggregation"
    # Buy 50. Buy another 50 at a different price. Sell 100.
    # Expected Result: The SELL engine must iterate over BOTH 
    # buy lots, calculate split P&L, and exhaust both quantities.
    # ---------------------------------------------------------
    add_trade('INFY', 'EQ', 'BUY', 50, 1400.00, 150)
    add_trade('INFY', 'EQ', 'BUY', 50, 1410.00, 180)
    add_trade('INFY', 'EQ', 'SELL', 100, 1450.00, 210)

    # Write to CSV
    with open(filename, mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(headers)
        writer.writerows(trades)

    print(f"✅ Successfully generated '{filename}' with {len(trades)} test records.")
    print("Scenarios included: Simple Match, Partial Sell, Complex FIFO Aggregation.")

if __name__ == "__main__":
    generate_test_data()