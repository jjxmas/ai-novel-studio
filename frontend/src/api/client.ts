import type { ApiResponse } from './types';

const baseUrl = '/api/v1';

export class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: number,
    readonly requestId?: string,
  ) {
    super(message);
    this.name = 'ApiRequestError';
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
  } catch (error) {
    throw new Error('NETWORK_UNAVAILABLE');
  }

  if (!response.ok) {
    throw await responseError(response, `HTTP_${response.status}`);
  }

  const payload = await parseApiResponse<T>(response);
  if (!payload.success) {
    throw new ApiRequestError(
      payload.message || '请求失败',
      response.status,
      payload.code,
      payload.requestId,
    );
  }

  return payload.data as T;
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
    throw await responseError(response, `DOWNLOAD_FAILED:${response.status}`);
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
    throw await responseError(response, `HTTP_${response.status}`);
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

async function responseError(response: Response, fallback: string): Promise<Error> {
  try {
    const payload = await parseApiResponse<unknown>(response.clone());
    if (payload.message) {
      return new ApiRequestError(payload.message, response.status, payload.code, payload.requestId);
    }
  } catch {
    // Non-JSON responses, such as proxy errors, use the HTTP fallback.
  }
  return new ApiRequestError(fallback, response.status);
}

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const payload: unknown = await response.json();
  if (!isApiResponse(payload)) {
    throw new ApiRequestError('INVALID_API_RESPONSE', response.status);
  }
  return payload as ApiResponse<T>;
}

function isApiResponse(payload: unknown): payload is ApiResponse<unknown> {
  if (typeof payload !== 'object' || payload === null) {
    return false;
  }
  const envelope = payload as Record<string, unknown>;
  return typeof envelope.code === 'number'
    && typeof envelope.message === 'string'
    && typeof envelope.success === 'boolean'
    && typeof envelope.timestamp === 'number'
    && typeof envelope.requestId === 'string';
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
