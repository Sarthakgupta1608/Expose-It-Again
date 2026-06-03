import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const path = request.nextUrl.pathname;

  const isPublicRoute = path === '/' || path === '/login' || path === '/register';
  const isAuthRoute = path === '/login' || path === '/register';

  const accessToken = request.cookies.get('access_token')?.value;
  const refreshToken = request.cookies.get('refresh_token')?.value;
  const hasToken = accessToken || refreshToken;

  if (!hasToken && !isPublicRoute)
    return NextResponse.redirect(new URL('/login', request.nextUrl));

  if (hasToken && isAuthRoute)
    return NextResponse.redirect(new URL('/dashboard', request.nextUrl));

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
}
