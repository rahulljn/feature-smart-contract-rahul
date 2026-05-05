'use client'

import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { segmentsApi } from '@/lib/api'
import type { Segment } from '@/types'

export function useSegments() {
  const [segments, setSegments] = useState<Segment[]>([])
  const [loading, setLoading] = useState(true)

  const { data } = useQuery({
    queryKey: ['segments'],
    queryFn: () => segmentsApi.list(),
    staleTime: 10 * 60 * 1000, // 10 minutes cache
  })

  useEffect(() => {
    if (data?.data?.data) {
      setSegments(data.data.data)
      setLoading(false)
    }
  }, [data])

  return { segments, loading }
}
