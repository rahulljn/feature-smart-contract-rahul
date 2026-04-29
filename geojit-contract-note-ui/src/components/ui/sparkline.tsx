"use client";

import { LineChart, Line, ResponsiveContainer } from "recharts";

export function SparklineChart({
  data,
  color,
  height = 50,
}: {
  data: number[];
  color: string;
  height?: number;
}) {
  const chartData = data.map((v, i) => ({ i, v }));
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData}>
        <Line
          type="monotone"
          dataKey="v"
          stroke={color}
          strokeWidth={1.5}
          dot={false}
          isAnimationActive={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
