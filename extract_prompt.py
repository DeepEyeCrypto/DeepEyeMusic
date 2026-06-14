import os
from collections import Counter

def main():
    pb_path = "/Users/enayat/.gemini/antigravity/conversations/0d3feace-1668-4f9c-b379-4b2b36bce3c1.pb"
    if not os.path.exists(pb_path):
        print("PB file not found.")
        return

    with open(pb_path, 'rb') as f:
        data = f.read(1000)
    
    print("First 100 bytes (hex):")
    print(data[:100].hex())
    
    print("\nFirst 100 bytes (ASCII/dots):")
    print("".join(chr(b) if 32 <= b < 127 else '.' for b in data[:100]))

    # Let's count byte frequencies in a larger sample (first 100KB)
    with open(pb_path, 'rb') as f:
        large_sample = f.read(100000)
    
    counter = Counter(large_sample)
    print("\nByte frequency of top 5 bytes in 100KB sample:")
    print(counter.most_common(5))
    
    # Calculate entropy or check if it's very flat (suggests encryption)
    import math
    entropy = 0
    for byte, count in counter.items():
        p = count / len(large_sample)
        entropy -= p * math.log2(p)
    print(f"Entropy of 100KB sample: {entropy:.4f} (max 8.0)")

if __name__ == "__main__":
    main()
