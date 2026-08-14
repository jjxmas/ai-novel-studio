import type { ApiResponse } from './types';

const baseUrl = '/api/v1';

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });
  } catch (error) {
    throw new Error('NETWORK_UNAVAILABLE');
  }

  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }

  const payload = (await response.json()) as ApiResponse<T>;
  if (!payload.success) {
    throw new Error(`BUSINESS_ERROR:${payload.message || '请求失败'}`);
  }

  return payload.data;
}

export function post<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export async function downloadRequest(path: string, body: unknown): Promise<Blob> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
  } catch (error) {
    throw new Error('NETWORK_UNAVAILABLE');
  }

  if (!response.ok) {
    throw new Error(`DOWNLOAD_FAILED:${response.status}`);
  }

  return response.blob();
}

export async function streamRequest<T>(
  path: string,
  body: unknown,
  onEvent: (event: T) => void,
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: 'POST',
      headers: {
        Accept: 'text/event-stream',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
  } catch (error) {
    throw new Error('NETWORK_UNAVAILABLE');
  }

  if (!response.ok || !response.body) {
    throw new Error(`璇锋眰澶辫触锛?{response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  const readEvent = (frame: string) => {
    const data = frame
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n');
    if (!data || data === '[DONE]') {
      return;
    }
    onEvent(JSON.parse(data) as T);
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const frames = buffer.split(/\r?\n\r?\n/);
    buffer = frames.pop() ?? '';
    frames.forEach(readEvent);
  }

  buffer += decoder.decode();
  if (buffer.trim()) {
    readEvent(buffer);
  }
}

export function patch<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PATCH',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export function del<T>(path: string): Promise<T> {
  return request<T>(path, {
    method: 'DELETE',
  });
}
